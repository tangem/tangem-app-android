package com.tangem.features.walletconnect.connections.model

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.domain.blockaid.models.dapp.CheckDAppResult
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.common.ui.account.CryptoPortfolioIconConverter
import com.tangem.common.ui.account.PortfolioSelectUM
import com.tangem.common.ui.account.toUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.core.ui.message.ToastMessage
import com.tangem.domain.account.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.walletconnect.WcAnalyticEvents
import com.tangem.domain.walletconnect.model.WcPairError
import com.tangem.domain.walletconnect.model.WcPairError.Unknown
import com.tangem.domain.walletconnect.model.WcPairRequest
import com.tangem.domain.walletconnect.model.WcSessionApprove
import com.tangem.domain.walletconnect.model.WcSessionProposal
import com.tangem.domain.walletconnect.model.sdkcopy.WcAppMetaData
import com.tangem.domain.walletconnect.usecase.pair.WcPairState
import com.tangem.domain.walletconnect.usecase.pair.WcPairUseCase
import com.tangem.features.account.PortfolioFetcher
import com.tangem.features.account.PortfolioSelectorComponent
import com.tangem.features.account.PortfolioSelectorController
import com.tangem.features.walletconnect.connections.components.WcPairComponent
import com.tangem.features.walletconnect.connections.components.WcSelectNetworksComponent
import com.tangem.features.walletconnect.connections.entity.WcAppInfoUM
import com.tangem.features.walletconnect.connections.entity.WcPrimaryButtonConfig
import com.tangem.features.walletconnect.connections.model.transformers.WcAppInfoTransformer
import com.tangem.features.walletconnect.connections.model.transformers.WcConnectButtonProgressTransformer
import com.tangem.features.walletconnect.connections.model.transformers.WcDAppVerifiedStateConverter
import com.tangem.features.walletconnect.connections.model.transformers.WcNetworksSelectedTransformer
import com.tangem.features.walletconnect.connections.routes.WcAppInfoRoutes
import com.tangem.features.walletconnect.impl.R
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.properties.Delegates
import com.tangem.utils.transformer.update as transformerUpdate

internal interface WcPairComponentCallback :
    WcSelectNetworksComponent.ModelCallback

@Stable
@ModelScoped
@Suppress("LongParameterList", "LargeClass")
internal class WcPairModel @Inject constructor(
    private val router: Router,
    private val messageSender: UiMessageSender,
    override val dispatchers: CoroutineDispatcherProvider,
    private val analytics: AnalyticsEventHandler,
    val selectorController: PortfolioSelectorController,
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase,
    portfolioFetcherFactory: PortfolioFetcher.Factory,
    wcPairUseCaseFactory: WcPairUseCase.Factory,
    paramsContainer: ParamsContainer,
) : Model(), WcPairComponentCallback {

    private val params: WcPairComponent.Params = paramsContainer.require()
    private val wcPairUseCase = wcPairUseCaseFactory.create(
        WcPairRequest(
            userWalletId = params.userWalletId,
            uri = params.wcUrl,
            source = params.source,
        ),
    )

    val stackNavigation = StackNavigation<WcAppInfoRoutes>()
    val portfolioFetcher: PortfolioFetcher = portfolioFetcherFactory.create(
        mode = PortfolioFetcher.Mode.All(isOnlyMultiCurrency = true),
        scope = modelScope,
    )
    val portfolioSelectorCallback = object : PortfolioSelectorComponent.BottomSheetCallback {
        override val onDismiss: () -> Unit = { stackNavigation.pop() }
        override val onBack: () -> Unit = { stackNavigation.pop() }
    }

    private val selectedPortfolio = MutableSharedFlow<Pair<UserWallet, AccountStatus.CryptoPortfolio>>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private var proposalNetwork by Delegates.notNull<WcSessionProposal.ProposalNetwork>()
    private var sessionProposal by Delegates.notNull<WcSessionProposal>()
    private var additionallyEnabledNetworks = emptySet<Network>()
    private val dAppVerifiedStateConverter = WcDAppVerifiedStateConverter(onVerifiedClick = ::showVerifiedAlert)

    val appInfoUiState: StateFlow<WcAppInfoUM>
        field = MutableStateFlow<WcAppInfoUM>(createLoadingState())

    init {
        modelScope.launch {
            val portfolioBalance = portfolioFetcher.data.first().balances
                .firstNotNullOfOrNull { (walletId, balance) ->
                    if (params.userWalletId == walletId) balance else null
                }
            if (portfolioBalance == null) {
                router.pop()
                return@launch
            }
            selectorController.selectAccount(portfolioBalance.accountsBalance.mainAccount.accountId)
            combineFlows(portfolioFetcher)
        }
    }

    private fun combineFlows(portfolioFetcher: PortfolioFetcher) {
        combine(
            flow = portfolioFetcher.data,
            flow2 = selectorController.selectedAccountWithData(portfolioFetcher)
                .filterNotNull()
                .onEach { selectedPortfolio.tryEmit(it) }
                .runningReduce { _, new ->
                    stackNavigation.pop()
                    new
                },
            flow3 = wcPairUseCase(),
            flow4 = isAccountsModeEnabledUseCase(),
            transform = { portfolios, selected, pairState, isAccountMode ->
                handlePairState(
                    pairState = pairState,
                    portfolios = portfolios,
                    selected = selected,
                    isAccountMode = isAccountMode,
                )
            },
        )
            .launchIn(modelScope)
    }

    private suspend fun handlePairState(
        pairState: WcPairState,
        portfolios: PortfolioFetcher.Data,
        selected: Pair<UserWallet, AccountStatus.CryptoPortfolio>,
        isAccountMode: Boolean,
    ) {
        when (pairState) {
            is WcPairState.Approving.Loading -> appInfoUiState.transformerUpdate(
                WcConnectButtonProgressTransformer(showProgress = true),
            )
            is WcPairState.Approving.Result -> {
                appInfoUiState.transformerUpdate(
                    WcConnectButtonProgressTransformer(showProgress = false),
                )
                pairState
                    .result
                    .onLeft(::processError)
                    .onRight(::processSuccessfullyConnected)
                router.pop()
            }
            is WcPairState.Error -> {
                appInfoUiState.transformerUpdate(
                    WcConnectButtonProgressTransformer(showProgress = false),
                )
                processError(pairState.error)
            }
            is WcPairState.Loading -> appInfoUiState.update { createLoadingState(isAccountMode == true) }
            is WcPairState.Proposal -> handleProposalState(
                pairState = pairState,
                portfolios = portfolios,
                selected = selected,
                isAccountMode = isAccountMode,
            )
        }
    }

    private suspend fun handleProposalState(
        pairState: WcPairState.Proposal,
        portfolios: PortfolioFetcher.Data,
        selected: Pair<UserWallet, AccountStatus.CryptoPortfolio>,
        isAccountMode: Boolean,
    ) {
        sessionProposal = pairState.dAppSession
        val portfolioAccount = selected.second
        val portfolioAccountId = portfolioAccount.account.accountId
        val proposalAccountNetwork = sessionProposal.proposalAccountNetwork
        val foundNetwork = proposalAccountNetwork[portfolioAccountId]
        if (foundNetwork == null) {
            processError(Unknown("Selected wallet not found"))
        } else {
            val portfolioSelectRow = createPortfolioSelectRow(
                selectedPortfolio = selected,
                portfolios = portfolios,
                isAccountMode = isAccountMode,
            )
            selectorController.isEnabled.value = { _, account ->
                proposalAccountNetwork.contains(account.account.accountId)
            }
            proposalNetwork = foundNetwork
            additionallyEnabledNetworks = proposalNetwork.available
            appInfoUiState.transformerUpdate(
                WcAppInfoTransformer(
                    dAppSession = sessionProposal,
                    dAppVerifiedStateConverter = dAppVerifiedStateConverter,
                    onDismiss = ::rejectPairing,
                    onConnect = ::onConnect,
                    portfolioSelectRow = portfolioSelectRow,
                    onNetworksClick = {
                        stackNavigation.pushNew(
                            WcAppInfoRoutes.SelectNetworks(
                                missingRequiredNetworks = proposalNetwork.missingRequired,
                                requiredNetworks = proposalNetwork.required,
                                availableNetworks = proposalNetwork.available,
                                enabledAvailableNetworks = additionallyEnabledNetworks,
                                notAddedNetworks = proposalNetwork.notAdded,
                            ),
                        )
                    },
                    proposalNetwork = proposalNetwork,
                    additionallyEnabledNetworks = additionallyEnabledNetworks,
                ),
            )
        }
    }

    private suspend fun createPortfolioSelectRow(
        selectedPortfolio: Pair<UserWallet, AccountStatus.CryptoPortfolio>,
        portfolios: PortfolioFetcher.Data,
        isAccountMode: Boolean,
    ): PortfolioSelectUM {
        val (wallet, portfolioAccount) = selectedPortfolio
        val account = when (val account = portfolioAccount.account) {
            is Account.CryptoPortfolio -> account
        }
        val icon: AccountIconUM.CryptoPortfolio?
        val name: TextReference
        if (isAccountMode) {
            icon = CryptoPortfolioIconConverter.convert(account.icon)
            name = account.accountName.toUM().value
        } else {
            icon = null
            name = stringReference(wallet.name)
        }
        return PortfolioSelectUM(
            icon = icon,
            name = name,
            isAccountMode = selectorController.isAccountMode.first(),
            isMultiChoice = !portfolios.isSingleChoice,
            onClick = { stackNavigation.pushNew(WcAppInfoRoutes.PortfolioSelector) },
        )
    }

    fun rejectPairing() {
        wcPairUseCase.reject()
    }

    fun connectFromAlert() {
        stackNavigation.pop()
        connect()
    }

    fun errorAlertOnDismiss() {
        router.pop()
    }

    private fun onConnect(securityStatus: CheckDAppResult) {
        when (securityStatus) {
            CheckDAppResult.SAFE -> connect()
            CheckDAppResult.UNSAFE -> showSecurityRiskAlert()
            CheckDAppResult.FAILED_TO_VERIFY -> showUnknownDomainAlert()
        }
    }

    private fun connect() {
        val enabledAvailableNetworks =
            proposalNetwork.available.filter { network -> network in additionallyEnabledNetworks }
        val selectedPortfolio = selectedPortfolio.replayCache
            .firstOrNull()
            ?: return
        val (wallet, account) = selectedPortfolio

        modelScope.launch {
            analytics.send(
                WcAnalyticEvents.PairButtonConnect(
                    dAppName = sessionProposal.dAppMetaData.name,
                    accountDerivation = account.account.derivationIndex.value,
                ),
            )
        }
        wcPairUseCase.approve(
            WcSessionApprove(
                wallet = wallet,
                network = enabledAvailableNetworks + proposalNetwork.required,
                account = account.account,
            ),
        )
    }

    private fun showUnknownDomainAlert() {
        val event = WcAnalyticEvents.NoticeSecurityAlert(
            dAppMetaData = sessionProposal.dAppMetaData,
            securityStatus = sessionProposal.securityStatus,
            source = WcAnalyticEvents.NoticeSecurityAlert.Source.Domain,
        )
        analytics.send(event)
        stackNavigation.pushNew(WcAppInfoRoutes.Alert.UnknownDomain)
    }

    private fun showSecurityRiskAlert() {
        val event = WcAnalyticEvents.NoticeSecurityAlert(
            dAppMetaData = sessionProposal.dAppMetaData,
            securityStatus = sessionProposal.securityStatus,
            source = WcAnalyticEvents.NoticeSecurityAlert.Source.Domain,
        )
        analytics.send(event)
        stackNavigation.pushNew(WcAppInfoRoutes.Alert.UnsafeDomain)
    }

    private fun showVerifiedAlert(appName: String) {
        stackNavigation.pushNew(WcAppInfoRoutes.Alert.Verified(appName))
    }

    private fun processSuccessfullyConnected(session: WcAppMetaData) {
        messageSender.send(
            SnackbarMessage(
                message = resourceReference(
                    id = R.string.wc_connected_to,
                    formatArgs = wrappedList(session.name),
                ),
            ),
        )
    }

    private fun processError(error: WcPairError) {
        val alert = when (error) {
            is WcPairError.InvalidDomainURL -> WcAppInfoRoutes.Alert.InvalidDomain
            is WcPairError.UnsupportedDApp -> WcAppInfoRoutes.Alert.UnsupportedDApp(error.appName)
            is WcPairError.UnsupportedBlockchains -> WcAppInfoRoutes.Alert.UnsupportedNetwork(error.appName)
            is WcPairError.UriAlreadyUsed -> WcAppInfoRoutes.Alert.UriAlreadyUsed
            is WcPairError.TimeoutException -> WcAppInfoRoutes.Alert.TimeoutException
            else -> {
                messageSender.send(ToastMessage(message = stringReference(error.message)))
                router.pop()
                null
            }
        }
        alert?.let { stackNavigation.pushNew(it) }
    }

    override fun onNetworksSelected(selectedNetworks: Set<Network>) {
        additionallyEnabledNetworks = selectedNetworks
        appInfoUiState.transformerUpdate(
            WcNetworksSelectedTransformer(
                missingNetworks = proposalNetwork.missingRequired,
                requiredNetworks = proposalNetwork.required,
                availableNetworks = proposalNetwork.available,
                notAddedNetworks = proposalNetwork.notAdded,
                additionallyEnabledNetworks = additionallyEnabledNetworks,
            ),
        )
    }

    private fun createLoadingState(isAccountMode: Boolean = false): WcAppInfoUM.Loading {
        return WcAppInfoUM.Loading(
            onDismiss = ::rejectPairing,
            connectButtonConfig = WcPrimaryButtonConfig(showProgress = false, enabled = false, onClick = {}),
            portfolioName = if (isAccountMode) {
                resourceReference(R.string.account_details_title)
            } else {
                resourceReference(R.string.wc_common_wallet)
            },
        )
    }
}