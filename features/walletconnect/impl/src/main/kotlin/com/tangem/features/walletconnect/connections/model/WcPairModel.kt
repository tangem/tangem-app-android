package com.tangem.features.walletconnect.connections.model

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.domain.blockaid.models.dapp.CheckDAppResult
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.core.ui.message.ToastMessage
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.walletconnect.WcAnalyticEvents
import com.tangem.domain.walletconnect.model.*
import com.tangem.domain.walletconnect.model.sdkcopy.WcAppMetaData
import com.tangem.domain.walletconnect.usecase.pair.WcPairState
import com.tangem.domain.walletconnect.usecase.pair.WcPairUseCase
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.walletconnect.connections.components.WcPairComponent
import com.tangem.features.walletconnect.connections.components.WcSelectNetworksComponent
import com.tangem.features.walletconnect.connections.components.WcSelectWalletComponent
import com.tangem.features.walletconnect.connections.entity.WcAppInfoUM
import com.tangem.features.walletconnect.connections.entity.WcPrimaryButtonConfig
import com.tangem.features.walletconnect.connections.model.transformers.*
import com.tangem.features.walletconnect.connections.routes.WcAppInfoRoutes
import com.tangem.features.walletconnect.impl.R
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import kotlin.properties.Delegates
import com.tangem.utils.transformer.update as transformerUpdate

internal interface WcPairComponentCallback :
    WcSelectWalletComponent.ModelCallback,
    WcSelectNetworksComponent.ModelCallback

private const val WC_WALLETS_SELECTOR_MIN_COUNT = 2

@Stable
@ModelScoped
@Suppress("LongParameterList")
internal class WcPairModel @Inject constructor(
    private val router: Router,
    private val messageSender: UiMessageSender,
    override val dispatchers: CoroutineDispatcherProvider,
    private val analytics: AnalyticsEventHandler,
    wcPairUseCaseFactory: WcPairUseCase.Factory,
    getWalletsUseCase: GetWalletsUseCase,
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

    private val selectedUserWalletFlow: MutableStateFlow<UserWallet> by lazy {
        MutableStateFlow(getWalletsUseCase.invokeSync().first { it.walletId == params.userWalletId })
    }
    private var proposalNetwork by Delegates.notNull<WcSessionProposal.ProposalNetwork>()
    private var sessionProposal by Delegates.notNull<WcSessionProposal>()
    private var additionallyEnabledNetworks = setOf<Network>()
    private val dAppVerifiedStateConverter = WcDAppVerifiedStateConverter(onVerifiedClick = ::showVerifiedAlert)

    val appInfoUiState: StateFlow<WcAppInfoUM>
        field = MutableStateFlow<WcAppInfoUM>(createLoadingState())

    init {
        loadDAppInfo()
    }

    private fun loadDAppInfo() {
        wcPairUseCase()
            .onEach { pairState ->
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
                    is WcPairState.Loading -> appInfoUiState.update { createLoadingState() }
                    is WcPairState.Proposal -> {
                        val availableWallets = pairState.dAppSession.proposalNetwork.keys
                            .filter { !it.isLocked && it.isMultiCurrency }
                        sessionProposal = pairState.dAppSession
                        proposalNetwork = sessionProposal.proposalNetwork.getValue(selectedUserWalletFlow.value)
                        additionallyEnabledNetworks = proposalNetwork.available
                        appInfoUiState.transformerUpdate(
                            WcAppInfoTransformer(
                                dAppSession = sessionProposal,
                                dAppVerifiedStateConverter = dAppVerifiedStateConverter,
                                onDismiss = ::rejectPairing,
                                onConnect = ::onConnect,
                                onWalletClick = {
                                    stackNavigation.pushNew(
                                        WcAppInfoRoutes.SelectWallet(selectedUserWalletFlow.value.walletId),
                                    )
                                }.takeIf { availableWallets.size >= WC_WALLETS_SELECTOR_MIN_COUNT },
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
                                userWallet = selectedUserWalletFlow.value,
                                proposalNetwork = proposalNetwork,
                                additionallyEnabledNetworks = additionallyEnabledNetworks,
                            ),
                        )
                    }
                }
            }
            .launchIn(modelScope)
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
        wcPairUseCase.approve(
            WcSessionApprove(
                wallet = selectedUserWalletFlow.value,
                network = enabledAvailableNetworks + proposalNetwork.required,
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

    override fun onWalletSelected(userWalletId: UserWalletId) {
        val selectedUserWallet = sessionProposal.proposalNetwork.keys.first { it.walletId == userWalletId }
        proposalNetwork = sessionProposal.proposalNetwork.getValue(selectedUserWallet)
        selectedUserWalletFlow.update { selectedUserWallet }
        additionallyEnabledNetworks = proposalNetwork.available
        appInfoUiState.transformerUpdate(
            WcAppInfoWalletChangedTransformer(
                selectedUserWallet = selectedUserWallet,
                proposalNetwork = proposalNetwork,
                additionallyEnabledNetworks = additionallyEnabledNetworks,
            ),
        )
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

    private fun createLoadingState(): WcAppInfoUM.Loading {
        return WcAppInfoUM.Loading(
            onDismiss = ::rejectPairing,
            connectButtonConfig = WcPrimaryButtonConfig(showProgress = false, enabled = false, onClick = {}),
        )
    }
}