package com.tangem.features.walletconnect.connections.model

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.tokens.GetWalletTotalBalanceUseCase
import com.tangem.domain.walletconnect.model.WcPairRequest
import com.tangem.domain.walletconnect.model.WcSessionApprove
import com.tangem.domain.walletconnect.model.WcSessionProposal
import com.tangem.domain.walletconnect.usecase.pair.WcPairState
import com.tangem.domain.walletconnect.usecase.pair.WcPairUseCase
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.GetCardImageUseCase
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.walletconnect.connections.components.WcAppInfoContainerComponent
import com.tangem.features.walletconnect.connections.entity.WcAppInfoUM
import com.tangem.features.walletconnect.connections.entity.WcAppInfoWalletUM
import com.tangem.features.walletconnect.connections.entity.WcPrimaryButtonConfig
import com.tangem.features.walletconnect.connections.model.transformers.WcAppInfoTransformer
import com.tangem.features.walletconnect.connections.model.transformers.WcAppInfoWalletChangedTransformer
import com.tangem.features.walletconnect.connections.model.transformers.WcConnectButtonProgressTransformer
import com.tangem.features.walletconnect.connections.routes.WcAppInfoRoutes
import com.tangem.features.walletconnect.connections.utils.WcUserWalletsFetcher
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import kotlin.properties.Delegates
import com.tangem.utils.transformer.update as transformerUpdate

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class WcAppInfoModel @Inject constructor(
    private val router: Router,
    override val dispatchers: CoroutineDispatcherProvider,
    wcPairUseCaseFactory: WcPairUseCase.Factory,
    getWalletsUseCase: GetWalletsUseCase,
    getWalletTotalBalanceUseCase: GetWalletTotalBalanceUseCase,
    getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    messageSender: UiMessageSender,
    getCardImageUseCase: GetCardImageUseCase,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params: WcAppInfoContainerComponent.Params = paramsContainer.require()
    private val wcPairUseCase = wcPairUseCaseFactory.create(
        WcPairRequest(
            userWalletId = params.userWalletId,
            uri = params.wcUrl,
            source = params.source,
        ),
    )
    private val userWalletsFetcher = WcUserWalletsFetcher(
        getWalletsUseCase = getWalletsUseCase,
        getWalletTotalBalanceUseCase = getWalletTotalBalanceUseCase,
        getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
        getBalanceHidingSettingsUseCase = getBalanceHidingSettingsUseCase,
        messageSender = messageSender,
        getCardImageUseCase = getCardImageUseCase,
        onWalletSelected = ::onWalletSelected,
    )

    private val selectedUserWalletFlow =
        MutableStateFlow(getWalletsUseCase.invokeSync().first { it.walletId == params.userWalletId })
    // TODO(wc) Doston: Temp solution, will be fixed in next PR`s
    private var proposalNetwork by Delegates.notNull<WcSessionProposal.ProposalNetwork>()
    internal var sessionProposal by Delegates.notNull<WcSessionProposal>()

    // UI states
    internal val contentNavigation = StackNavigation<WcAppInfoRoutes>()
    val appInfoUiState: StateFlow<WcAppInfoUM>
    field = MutableStateFlow<WcAppInfoUM>(createLoadingState())
    val walletsUiState: StateFlow<WcAppInfoWalletUM>
    field = MutableStateFlow(WcAppInfoWalletUM(persistentListOf(), selectedUserWalletFlow.value))

    init {
        loadDAppInfo()
        combine(
            flow = userWalletsFetcher.userWallets,
            flow2 = selectedUserWalletFlow,
            transform = ::updateWalletsState,
        ).launchIn(modelScope)
    }

    private fun loadDAppInfo() {
        wcPairUseCase()
            .onEach { state ->
                when (state) {
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
                        // TODO: wc show toast/snackbar/alert?
                    }
                    is WcPairState.Loading -> appInfoUiState.update { createLoadingState() }
                    is WcPairState.Proposal -> {
                        sessionProposal = state.dAppSession
                        proposalNetwork = sessionProposal.proposalNetwork.getValue(selectedUserWalletFlow.value)
                        appInfoUiState.transformerUpdate(
                            WcAppInfoTransformer(
                                dAppSession = state.dAppSession,
                                onDismiss = ::dismiss,
                                onConnect = ::onConnect,
                                onWalletClick = { contentNavigation.pushNew(WcAppInfoRoutes.SelectWallet) },
                                userWallet = selectedUserWalletFlow.value,
                                proposalNetwork = proposalNetwork,
                            ),
                        )
                    }
                }
            }
            .launchIn(modelScope)
    }

    fun dismiss() {
        wcPairUseCase.reject()
        router.pop()
    }

    private fun onConnect() {
        wcPairUseCase.approve(
            WcSessionApprove(
                wallet = selectedUserWalletFlow.value,
                network = proposalNetwork.required.plus(proposalNetwork.available).toList(),
            ),
        )
    }

    private fun onWalletSelected(userWalletId: UserWalletId) {
        val selectedUserWallet = sessionProposal.proposalNetwork.keys.first { it.walletId == userWalletId }
        selectedUserWalletFlow.update { selectedUserWallet }
        proposalNetwork = sessionProposal.proposalNetwork.getValue(selectedUserWallet)
        appInfoUiState.transformerUpdate(WcAppInfoWalletChangedTransformer(selectedUserWallet, proposalNetwork))
        contentNavigation.pop()
    }

    private fun updateWalletsState(items: ImmutableList<UserWalletItemUM>, userWallet: UserWallet) {
        walletsUiState.update { state -> state.copy(wallets = items, selectedUserWallet = userWallet) }
    }

    private fun createLoadingState(): WcAppInfoUM.Loading {
        return WcAppInfoUM.Loading(
            onDismiss = ::dismiss,
            connectButtonConfig = WcPrimaryButtonConfig(showProgress = false, enabled = false, onClick = ::onConnect),
        )
    }
}