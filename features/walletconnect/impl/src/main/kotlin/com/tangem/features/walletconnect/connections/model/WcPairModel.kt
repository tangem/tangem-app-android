package com.tangem.features.walletconnect.connections.model

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.domain.blockaid.models.dapp.CheckDAppResult
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.ToastMessage
import com.tangem.domain.models.network.Network
import com.tangem.domain.walletconnect.model.WcPairError
import com.tangem.domain.walletconnect.model.WcPairRequest
import com.tangem.domain.walletconnect.model.WcSessionApprove
import com.tangem.domain.walletconnect.model.WcSessionProposal
import com.tangem.domain.walletconnect.usecase.pair.WcPairState
import com.tangem.domain.walletconnect.usecase.pair.WcPairUseCase
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.walletconnect.connections.components.WcPairComponent
import com.tangem.features.walletconnect.connections.components.WcSelectNetworksComponent
import com.tangem.features.walletconnect.connections.components.WcSelectWalletComponent
import com.tangem.features.walletconnect.connections.entity.WcAppInfoUM
import com.tangem.features.walletconnect.connections.entity.WcPrimaryButtonConfig
import com.tangem.features.walletconnect.connections.model.transformers.*
import com.tangem.features.walletconnect.connections.routes.WcAppInfoRoutes
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import kotlin.properties.Delegates
import com.tangem.utils.transformer.update as transformerUpdate

internal interface WcPairComponentCallback :
    WcSelectWalletComponent.ModelCallback,
    WcSelectNetworksComponent.ModelCallback

@Stable
@ModelScoped
internal class WcPairModel @Inject constructor(
    private val router: Router,
    private val messageSender: UiMessageSender,
    override val dispatchers: CoroutineDispatcherProvider,
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

    private val selectedUserWalletFlow =
        MutableStateFlow(getWalletsUseCase.invokeSync().first { it.walletId == params.userWalletId })
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
        fun errorToast(error: WcPairError) = messageSender.send(ToastMessage(message = stringReference(error.message)))
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
                        pairState.result.onLeft(::errorToast)
                        router.pop()
                    }
                    is WcPairState.Error -> {
                        appInfoUiState.transformerUpdate(
                            WcConnectButtonProgressTransformer(showProgress = false),
                        )
                        errorToast(pairState.error)
                    }
                    is WcPairState.Loading -> appInfoUiState.update { createLoadingState() }
                    is WcPairState.Proposal -> {
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
                                },
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
        stackNavigation.pushNew(WcAppInfoRoutes.Alert(WcAppInfoRoutes.Alert.Type.UnknownDomain))
    }

    private fun showSecurityRiskAlert() {
        stackNavigation.pushNew(WcAppInfoRoutes.Alert(WcAppInfoRoutes.Alert.Type.UnsafeDomain))
    }

    private fun showVerifiedAlert(appName: String) {
        stackNavigation.pushNew(WcAppInfoRoutes.Alert(WcAppInfoRoutes.Alert.Type.Verified(appName)))
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
