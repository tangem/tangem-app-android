package com.tangem.features.jointaccount.join.invitepreview.model

import androidx.compose.runtime.Stable
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.account.status.usecase.GetWalletTotalBalanceUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isLocked
import com.tangem.features.jointaccount.join.invitepreview.state.JointAccountInvitePreviewStateController
import com.tangem.features.jointaccount.join.invitepreview.state.transformers.UpdateInvitePreviewInitialStateTransformer
import com.tangem.features.jointaccount.join.invitepreview.state.transformers.UpdateInvitePreviewWalletsTransformer
import com.tangem.features.jointaccount.join.invitepreview.ui.state.JointAccountInvitePreviewUM
import com.tangem.features.jointaccount.join.model.JointAccountJoinChildParams
import com.tangem.features.jointaccount.join.navigation.JointAccountJoinRoute
import com.tangem.features.wallet.utils.UserWalletImageFetcher
import com.tangem.operations.attestation.ArtworkSize
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class JointAccountInvitePreviewModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val stateController: JointAccountInvitePreviewStateController,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val getWalletTotalBalanceUseCase: GetWalletTotalBalanceUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val userWalletImageFetcher: UserWalletImageFetcher,
) : Model() {

    private val params = paramsContainer.require<JointAccountJoinChildParams>()

    private val selectedWalletId = MutableStateFlow(
        value = params
            .draftHolder
            .draft
            .value
            .selectedWalletId,
    )

    private val isChooseWalletShown = MutableStateFlow(value = false)

    val uiState: StateFlow<JointAccountInvitePreviewUM>
        get() = stateController.uiState

    init {
        updateInitialState()
        observeWallets()
    }

    private fun updateInitialState() {
        stateController.update(
            UpdateInvitePreviewInitialStateTransformer(
                onCreatorInfoClick = ::onCreatorInfoClick,
                onContinueClick = ::onContinueClick,
                onCloseClick = ::onCloseClick,
            ),
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeWallets() {
        combine(
            flow = userWalletsListRepository.userWallets
                .map { wallets -> wallets.orEmpty().filterNot(UserWallet::isLocked) }
                .onEach { wallets ->
                    if (selectedWalletId.value == null) {
                        selectedWalletId.value = defaultWalletId(wallets = wallets)
                    }
                },
            flow2 = selectedWalletId.filterNotNull(),
            flow3 = isChooseWalletShown,
            flow4 = getSelectedAppCurrencyUseCase.invokeOrDefault(),
            flow5 = getBalanceHidingSettingsUseCase.isBalanceHidden(),
            transform = ::WalletsState,
        )
            .flatMapLatest { state ->
                combine(
                    flow = getWalletTotalBalanceUseCase(userWalletIds = state.wallets.map(UserWallet::walletId))
                        .map { lce -> lce.getOrNull().orEmpty() },
                    flow2 = userWalletImageFetcher.walletsImage(wallets = state.wallets, size = ArtworkSize.SMALL),
                ) { balances, images -> createWalletsTransformer(state, balances, images) }
            }
            .onEach(stateController::update)
            .flowOn(dispatchers.default)
            .launchIn(modelScope)
    }

    private fun defaultWalletId(wallets: List<UserWallet>): UserWalletId? {
        val currentWalletId = userWalletsListRepository.selectedUserWallet.value?.walletId

        return wallets.firstOrNull { it.walletId == currentWalletId }?.walletId
            ?: wallets.firstOrNull()?.walletId
    }

    private fun createWalletsTransformer(
        state: WalletsState,
        balances: Map<UserWalletId, TotalFiatBalance>,
        images: Map<UserWalletId, UserWalletItemUM.ImageState>,
    ): UpdateInvitePreviewWalletsTransformer = UpdateInvitePreviewWalletsTransformer(
        walletsInfo = UpdateInvitePreviewWalletsTransformer.WalletsInfo(
            wallets = state.wallets,
            selectedWalletId = state.selectedId,
            isSheetShown = state.isSheetShown,
            balances = balances,
            images = images,
            appCurrency = state.appCurrency,
            isBalanceHidden = state.isBalanceHidden,
        ),
        intents = UpdateInvitePreviewWalletsTransformer.Intents(
            onWalletSelect = ::onWalletSelect,
            onWalletRowClick = ::onWalletRowClick,
            onChooseWalletDismiss = ::onChooseWalletDismiss,
        ),
    )

    private fun onCreatorInfoClick() {
        // TODO: open the member card of the creator — [REDACTED_TASK_KEY]
    }

    private fun onWalletRowClick() {
        isChooseWalletShown.value = true
    }

    private fun onChooseWalletDismiss() {
        isChooseWalletShown.value = false
    }

    private fun onWalletSelect(walletId: UserWalletId) {
        selectedWalletId.value = walletId
        isChooseWalletShown.value = false
    }

    private fun onContinueClick() {
        val walletId = selectedWalletId.value ?: return

        params.draftHolder.setSelectedWallet(walletId)
        router.push(JointAccountJoinRoute.DisplayName)
    }

    private fun onCloseClick() {
        router.pop()
    }

    private data class WalletsState(
        val wallets: List<UserWallet>,
        val selectedId: UserWalletId,
        val isSheetShown: Boolean,
        val appCurrency: AppCurrency,
        val isBalanceHidden: Boolean,
    )
}