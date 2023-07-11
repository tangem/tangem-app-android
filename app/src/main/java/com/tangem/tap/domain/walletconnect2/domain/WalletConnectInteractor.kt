package com.tangem.tap.domain.walletconnect2.domain

import com.tangem.core.analytics.Analytics
import com.tangem.tap.common.analytics.events.WalletConnect
import com.tangem.tap.common.extensions.filterNotNull
import com.tangem.tap.domain.walletconnect.WalletConnectSdkHelper
import com.tangem.tap.domain.walletconnect2.domain.models.*
import com.tangem.tap.features.details.ui.walletconnect.WcSessionForScreen
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber

class WalletConnectInteractor(
    private val handler: WalletConnectEventsHandler,
    private val walletConnectRepository: WalletConnectRepository,
    private val sessionsRepository: WalletConnectSessionsRepository,
    private val sdkHelper: WalletConnectSdkHelper,
    private val dispatcher: CoroutineDispatcherProvider,
    val blockchainHelper: WcBlockchainHelper,
) {

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

    suspend fun startListening(userWalletId: String, cardId: String?) {
        this.userWalletId = userWalletId
        this.cardId = cardId
        walletConnectRepository.updateSessions()
        coroutineScope {
            launch { subscribeToEvents() }
            launch { subscribeToSessions() }
        }
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
                                val missingNetworks = wcEvent.error.missingChains
                                    .map { blockchainHelper.chainIdToNetworkIdOrNull(it) }
                                WalletConnectError.ApprovalErrorAddNetwork(missingNetworks.filterNotNull())
                            }
                            else -> wcEvent.error
                        }
                        handler.onSessionRejected(error)
                    }
                    is WalletConnectEvents.SessionApprovalSuccess -> {
                        sessionsRepository.saveSession(
                            userWallet = userWalletId,
                            session = Session.fromAccounts(
                                accounts = wcEvent.accounts,
                                topic = wcEvent.topic,
                            ),
                        )
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
            .flowOn(dispatcher.io)
            .collect()
    }

    private suspend fun subscribeToSessions() {
        sessions
            .onEach { listOfSessions ->
                val relevantTopics = getTopicsForUserWallet(userWalletId, sessionsRepository)
                val filteredSessions = filterSessionsForUserWallet(listOfSessions, relevantTopics)
                handler.onListOfSessionsUpdated(filteredSessions)
            }
            .flowOn(dispatcher.io)
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

    fun rejectRequest(topic: String, id: Long) {
        walletConnectRepository.rejectRequest(topic, id)
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
        if (error != null) {
            walletConnectRepository.rejectRequest(sessionRequest.topic, sessionRequest.id)
            return
        }

        when (sessionRequest.request) {
            is WcRequest.BnbCancel -> Unit
            is WcRequest.BnbTxConfirm -> walletConnectRepository.sendRequest(
                topic = sessionRequest.topic,
                id = sessionRequest.id,
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

        val networkId = blockchainHelper.chainIdToNetworkIdOrNull(currentRequest.chainId ?: "") ?: return
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
                derivationPath = request.derivationPath,
                cardId = cardId,
            )
        }

        Timber.d("Signed hash: $signedHash")

        if (signedHash == null) {
            walletConnectRepository.rejectRequest(
                topic = request.topic,
                id = request.requestId,
            )
        } else {
            Analytics.send(WalletConnect.RequestSigned())
            walletConnectRepository.sendRequest(
                topic = request.topic,
                id = request.requestId,
                result = signedHash,
            )
        }
    }

    private suspend fun prepareRequestData(sessionRequest: WalletConnectEvents.SessionRequest): WcPreparedRequest? {
        return sessionRequestConverter.prepareRequest(sessionRequest, userWalletId)
    }
}