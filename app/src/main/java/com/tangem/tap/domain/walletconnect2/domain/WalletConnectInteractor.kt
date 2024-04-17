package com.tangem.tap.domain.walletconnect2.domain

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.legacy.WalletsStateHolder
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.usecase.GetSelectedWalletUseCase
import com.tangem.tap.common.extensions.filterNotNull
import com.tangem.tap.domain.walletconnect.WalletConnectSdkHelper
import com.tangem.tap.domain.walletconnect2.domain.models.*
import com.tangem.tap.features.details.ui.walletconnect.WcSessionForScreen
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.FeatureCoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

@Suppress("LargeClass", "LongParameterList")
class WalletConnectInteractor(
    private val handler: WalletConnectEventsHandler,
    private val walletConnectRepository: WalletConnectRepository,
    private val sessionsRepository: WalletConnectSessionsRepository,
    private val sdkHelper: WalletConnectSdkHelper,
    private val dispatchers: CoroutineDispatcherProvider,
    private val walletManagersFacade: WalletManagersFacade,
    private val currenciesRepository: CurrenciesRepository,
    private val walletsStateHolder: WalletsStateHolder,
    val blockchainHelper: WcBlockchainHelper,
) {

    private val getSelectedWalletUseCase by lazy(LazyThreadSafetyMode.NONE) {
        GetSelectedWalletUseCase(walletsStateHolder)
    }

    private val wcScope = CoroutineScope(
        Job() + dispatchers.io + FeatureCoroutineExceptionHandler.create("wcScope"),
    )

    private val listenerScope = CoroutineScope(
        Job() + dispatchers.io + FeatureCoroutineExceptionHandler.create("listenScope"),
    )

    private val events = walletConnectRepository.events
    private val sessions = walletConnectRepository.activeSessions

    private var userWalletId: String = ""
    private var cardId: String? = null

    private var currentRequest: WalletConnectEvents.SessionRequest? = null
    private val sessionRequestConverter = WcSessionRequestConverter(
        blockchainHelper = blockchainHelper,
        sessionsRepository = sessionsRepository,
        sdkHelper = sdkHelper,
    )

    init {
        getSelectedWalletUseCase().onRight { userWalletFlow ->
            userWalletFlow
                .conflate()
                .distinctUntilChanged()
                .onEach(::initWithWallet)
                .flowOn(dispatchers.io)
                .launchIn(wcScope)
        }
    }

    private suspend fun initWithWallet(userWallet: UserWallet) {
        if (userWallet.isMultiCurrency) {
            Timber.d("WalletConnect: initialize and setup networks for ${userWallet.walletId}")
            startListeningWc(userWallet.walletId.stringValue, getCardId(userWallet))
            subscribeOnCurrenciesUpdates(userWallet)
        }
    }

    private fun subscribeOnCurrenciesUpdates(userWallet: UserWallet) {
        currenciesRepository.getMultiCurrencyWalletCurrenciesUpdates(userWallet.walletId)
            .conflate()
            .distinctUntilChanged()
            .onEach { currencies ->
                setupUserChains(userWallet, currencies)
            }
            .flowOn(dispatchers.io)
            .launchIn(wcScope)
    }

    private suspend fun setupUserChains(userWallet: UserWallet, currencies: List<CryptoCurrency>) {
        val accounts = getAccountsForWc(
            userWallet = userWallet,
            networks = currencies.map { it.network },
        )
        setUserChains(accounts)
    }

    private suspend fun startListeningWc(userWalletId: String, cardId: String?) {
        this.userWalletId = userWalletId
        this.cardId = cardId
        listenerScope.coroutineContext.cancelChildren()
        listenerScope.launch {
            launch { subscribeToEvents() }
            launch { subscribeToSessions() }
            walletConnectRepository.updateSessions()
        }
    }

    private fun setUserChains(accounts: List<Account>) {
        val userNamespaces: Map<NetworkNamespace, List<Account>> = accounts
            .groupBy { account ->
                blockchainHelper.getNamespaceFromFullChainIdOrNull(account.chainId)
                    ?.let { NetworkNamespace(it) }
            }.filterNotNull()
        walletConnectRepository.setUserNamespaces(userNamespaces)
    }

    private suspend fun subscribeToEvents() {
        events
            .onEach { wcEvent ->
                when (wcEvent) {
                    is WalletConnectEvents.SessionProposal -> {
                        Timber.d("WC session proposal event received")
                        val unsupportedNetworks = wcEvent.requiredChainIds
                            .filter { blockchainHelper.chainIdToNetworkIdOrNull(it) == null }
                        if (unsupportedNetworks.isNotEmpty()) {
                            val error = WalletConnectError.ApprovalErrorUnsupportedNetwork(unsupportedNetworks)
                            handler.onSessionRejected(error)
                            return@onEach
                        }
                        val networksFormatted = (wcEvent.requiredChainIds + wcEvent.optionalChainIds)
                            .distinct()
                            .mapNotNull { blockchainHelper.chainIdToFullNameOrNull(it) }
                            .toString()
                        handler.onProposalReceived(proposal = wcEvent, networksFormatted = networksFormatted)
                    }
                    is WalletConnectEvents.SessionApprovalError -> {
                        val error = when (wcEvent.error) {
                            is WalletConnectError.ApprovalErrorMissingNetworks -> {
                                val missingNetworks = wcEvent.error.missingChains.mapNotNull {
                                    blockchainHelper.chainIdToMissingNetworkNameOrNull(it)
                                }

                                WalletConnectError.ApprovalErrorAddNetwork(missingNetworks)
                            }
                            else -> wcEvent.error
                        }
                        handler.onSessionRejected(error)
                    }
                    is WalletConnectEvents.SessionApprovalSuccess -> {
                        sessionsRepository.saveSession(
                            userWallet = userWalletId,
                            session = Session(
                                accounts = wcEvent.accounts,
                                topic = wcEvent.topic,
                            ),
                        )
                        walletConnectRepository.updateSessions()
                        handler.onSessionEstablished()
                    }
                    is WalletConnectEvents.SessionDeleted -> {
                        sessionsRepository.removeSession(userWalletId, wcEvent.topic)
                    }
                    is WalletConnectEvents.SessionRequest -> {
                        handleRequest(wcEvent)
                    }
                }
            }
            .flowOn(dispatchers.io)
            .collect()
    }

    private suspend fun subscribeToSessions() {
        sessions
            .onEach { listOfSessions ->
                val relevantTopics = getTopicsForUserWallet(userWalletId, sessionsRepository)
                val filteredSessions = filterSessionsForUserWallet(listOfSessions, relevantTopics)
                handler.onListOfSessionsUpdated(filteredSessions)
            }
            .flowOn(dispatchers.io)
            .collect()
    }

    private fun filterSessionsForUserWallet(
        availableSessions: List<WalletConnectSession>,
        relevantTopics: List<String>,
    ): List<WcSessionForScreen> {
        return availableSessions.filter { relevantTopics.contains(it.topic) }
            .map {
                WcSessionForScreen(
                    description = it.name ?: "",
                    sessionId = it.topic,
                )
            }
    }

    private suspend fun getTopicsForUserWallet(
        userWalletId: String,
        repository: WalletConnectSessionsRepository,
    ): List<String> {
        return repository.loadSessions(userWalletId).map { it.topic }
    }

    fun approveSessionProposal(accounts: List<Account>) {
        val userNamespaces: Map<NetworkNamespace, List<Account>> = accounts
            .groupBy { account ->
                blockchainHelper.getNamespaceFromFullChainIdOrNull(account.chainId)
                    ?.let { NetworkNamespace(it) }
            }.filterNotNull()
        walletConnectRepository.approve(
            userNamespaces = userNamespaces,
        )
    }

    fun rejectSessionProposal() {
        walletConnectRepository.reject()
    }

    fun disconnectSession(topic: String) {
        walletConnectRepository.disconnect(topic)
    }

    fun cancelRequest(topic: String, id: Long) {
        walletConnectRepository.cancelRequest(topic, id)
    }

    private suspend fun handleRequest(sessionRequest: WalletConnectEvents.SessionRequest) {
        val error: WalletConnectError? = when {
            sessionsRepository.loadSessions(userWalletId).none { it.topic == sessionRequest.topic } -> {
                WalletConnectError.WrongUserWallet
            }
            sessionRequest.request is WcRequest.CustomRequest -> {
                WalletConnectError.UnsupportedMethod
            }
            else -> {
                null
            }
        }
        val networkId = sessionRequest.chainId?.let { blockchainHelper.chainIdToNetworkIdOrNull(it) } ?: ""
        val requestData = RequestData(
            topic = sessionRequest.topic,
            requestId = sessionRequest.id,
            blockchain = networkId,
            method = sessionRequest.method,
        )

        if (error != null) {
            walletConnectRepository.rejectRequest(requestData, error)
            return
        }

        when (sessionRequest.request) {
            is WcRequest.BnbCancel -> Unit
            is WcRequest.BnbTxConfirm -> walletConnectRepository.sendRequest(
                requestData = requestData,
                result = "",
            )
            else -> {
                currentRequest = sessionRequest
                val data = prepareRequestData(sessionRequest)
                if (data != null) {
                    handler.onSessionRequest(data)
                }
            }
        }
    }

    suspend fun continueWithRequest(request: WcPreparedRequest) {
        val currentRequest = this.currentRequest
        if (currentRequest == null || request.topic != currentRequest.topic) return

        val networkId = blockchainHelper.chainIdToNetworkIdOrNull(currentRequest.chainId.orEmpty()) ?: return
        val signedHash = when (request) {
            is WcPreparedRequest.BnbTransaction -> sdkHelper.signBnbTransaction(
                data = request.preparedRequestData.data.data,
                networkId = networkId,
                derivationPath = request.derivationPath,
                cardId = cardId,
            )
            is WcPreparedRequest.EthTransaction -> sdkHelper.completeTransaction(
                data = request.preparedRequestData,
                cardId = cardId,
            )
            is WcPreparedRequest.EthSign -> sdkHelper.signPersonalMessage(
                hashToSign = request.preparedRequestData.hash,
                networkId = networkId,
                type = request.preparedRequestData.type,
                derivationPath = request.derivationPath,
                cardId = cardId,
            )
            is WcPreparedRequest.SignTransaction -> sdkHelper.signTransaction(
                hashToSign = request.preparedRequestData.hashToSign,
                networkId = networkId,
                type = request.preparedRequestData.type,
                derivationPath = request.derivationPath,
                cardId = cardId,
            )
        }

        val requestData = RequestData(
            topic = request.topic,
            requestId = request.requestId,
            blockchain = networkId,
            method = currentRequest.method,
        )

        if (signedHash == null) {
            walletConnectRepository.rejectRequest(requestData, WalletConnectError.SigningError)
        } else {
            walletConnectRepository.sendRequest(
                requestData = requestData,
                result = signedHash,
            )
        }
    }

    fun isWalletConnectUri(uri: String): Boolean {
        return uri.lowercase().startsWith(WC_SCHEME)
    }

    private fun getCardId(userWallet: UserWallet): String? {
        return if (userWallet.scanResponse.card.backupStatus?.isActive != true) {
            userWallet.cardId
        } else { // if wallet has backup, any card from wallet can be used to sign
            null
        }
    }

    private suspend fun getAccountsForWc(userWallet: UserWallet, networks: List<Network>): List<Account> {
        val walletManagers = networks.mapNotNull {
            val blockchain = Blockchain.fromId(it.id.value)
            walletManagersFacade.getOrCreateWalletManager(
                userWalletId = userWallet.walletId,
                blockchain = blockchain,
                derivationPath = it.derivationPath.value,
            )
        }
        return walletManagers.mapNotNull {
            val wallet = it.wallet
            val chainId = blockchainHelper.networkIdToChainIdOrNull(
                wallet.blockchain.toNetworkId(),
            )
            chainId?.let {
                Account(
                    chainId,
                    wallet.address,
                    wallet.publicKey.derivationPath?.rawPath,
                )
            }
        }
    }

    private suspend fun prepareRequestData(sessionRequest: WalletConnectEvents.SessionRequest): WcPreparedRequest? {
        return sessionRequestConverter.prepareRequest(sessionRequest, userWalletId)
    }

    companion object {
        private const val WC_SCHEME = "wc"
    }
}
