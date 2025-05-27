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
import com.tangem.core.ui.R
import com.tangem.core.ui.components.bottomsheets.message.*
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.models.network.Network
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
import com.tangem.features.walletconnect.connections.model.transformers.WcAppInfoTransformer
import com.tangem.features.walletconnect.connections.model.transformers.WcAppInfoWalletChangedTransformer
import com.tangem.features.walletconnect.connections.model.transformers.WcConnectButtonProgressTransformer
import com.tangem.features.walletconnect.connections.model.transformers.WcDAppVerifiedStateConverter
import com.tangem.features.walletconnect.connections.routes.WcAppInfoRoutes
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import timber.log.Timber
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
    private val additionallyEnabledNetworks = mutableSetOf<Network.RawID>()
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
                        router.pop()
                    }
                    is WcPairState.Error -> {
                        Timber.e(pairState.error)
                    }
                    is WcPairState.Loading -> appInfoUiState.update { createLoadingState() }
                    is WcPairState.Proposal -> {
                        sessionProposal = pairState.dAppSession
                        proposalNetwork = sessionProposal.proposalNetwork.getValue(selectedUserWalletFlow.value)
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

    private fun onConnect(securityStatus: CheckDAppResult) {
        when (securityStatus) {
            CheckDAppResult.SAFE -> connect()
            CheckDAppResult.UNSAFE -> showSecurityRiskAlert()
            CheckDAppResult.FAILED_TO_VERIFY -> showUnknownDomainAlert()
        }
    }

    private fun connect() {
        val enabledAvailableNetworks =
            proposalNetwork.available.filter { network -> network.id.rawId in additionallyEnabledNetworks }
        wcPairUseCase.approve(
            WcSessionApprove(
                wallet = selectedUserWalletFlow.value,
                network = enabledAvailableNetworks + proposalNetwork.required,
            ),
        )
    }

    private fun connectFromAlert() {
        stackNavigation.pop()
        connect()
    }

    private fun showUnknownDomainAlert() {
        val message = messageBottomSheetUM {
            infoBlock {
                icon(R.drawable.img_knight_shield_32) {
                    type = MessageBottomSheetUMV2.Icon.Type.Attention
                    backgroundType = MessageBottomSheetUMV2.Icon.BackgroundType.SameAsTint
                }
                title = resourceReference(R.string.security_alert_title)
                body = resourceReference(R.string.wc_alert_domain_issues_description)
                chip(resourceReference(R.string.wc_alert_audit_unknown_domain)) {
                    type = MessageBottomSheetUMV2.Chip.Type.Unspecified
                }
            }
            primaryButton {
                text = resourceReference(R.string.common_cancel)
                onClick { closeBs() }
            }
            secondaryButton {
                text = resourceReference(R.string.wc_alert_connect_anyway)
                onClick { connectFromAlert() }
            }
        }
        stackNavigation.pushNew(WcAppInfoRoutes.Alert(elements = message.elements))
    }

    private fun showSecurityRiskAlert() {
        val message = messageBottomSheetUM {
            infoBlock {
                icon(R.drawable.img_knight_shield_32) {
                    type = MessageBottomSheetUMV2.Icon.Type.Warning
                    backgroundType = MessageBottomSheetUMV2.Icon.BackgroundType.SameAsTint
                }
                title = resourceReference(R.string.security_alert_title)
                body = resourceReference(R.string.wc_alert_domain_issues_description)
                chip(resourceReference(R.string.wc_alert_audit_unknown_domain)) {
                    type = MessageBottomSheetUMV2.Chip.Type.Warning
                }
            }
            primaryButton {
                text = resourceReference(R.string.common_cancel)
                onClick { closeBs() }
            }
            secondaryButton {
                text = resourceReference(R.string.wc_alert_connect_anyway)
                onClick { connectFromAlert() }
            }
        }
        stackNavigation.pushNew(WcAppInfoRoutes.Alert(elements = message.elements))
    }

    private fun showVerifiedAlert(appName: String) {
        val message = messageBottomSheetUM {
            infoBlock {
                icon(R.drawable.img_approvale2_20) {
                    type = MessageBottomSheetUMV2.Icon.Type.Accent
                    backgroundType = MessageBottomSheetUMV2.Icon.BackgroundType.Unspecified
                }
                title = resourceReference(R.string.wc_alert_verified_domain_title)
                body = resourceReference(R.string.wc_alert_verified_domain_description, wrappedList(appName))
            }
            secondaryButton {
                text = resourceReference(R.string.common_done)
                onClick { closeBs() }
            }
        }
        stackNavigation.pushNew(WcAppInfoRoutes.Alert(elements = message.elements))
    }

    override fun onWalletSelected(userWalletId: UserWalletId) {
        val selectedUserWallet = sessionProposal.proposalNetwork.keys.first { it.walletId == userWalletId }
        proposalNetwork = sessionProposal.proposalNetwork.getValue(selectedUserWallet)
        selectedUserWalletFlow.update { selectedUserWallet }
        appInfoUiState.transformerUpdate(WcAppInfoWalletChangedTransformer(selectedUserWallet, proposalNetwork))
        additionallyEnabledNetworks.clear()
    }

    override fun onNetworksSelected(selectedNetworks: Set<Network.RawID>) {
        additionallyEnabledNetworks.clear()
        additionallyEnabledNetworks.addAll(selectedNetworks)
    }

    private fun createLoadingState(): WcAppInfoUM.Loading {
        return WcAppInfoUM.Loading(
            onDismiss = ::rejectPairing,
            connectButtonConfig = WcPrimaryButtonConfig(showProgress = false, enabled = false, onClick = {}),
        )
    }
}