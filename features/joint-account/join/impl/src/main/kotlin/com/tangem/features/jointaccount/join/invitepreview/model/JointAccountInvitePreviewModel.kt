package com.tangem.features.jointaccount.join.invitepreview.model

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.isLocked
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioFetcher
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorComponent
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorController
import com.tangem.features.jointaccount.join.invitepreview.state.JointAccountInvitePreviewStateController
import com.tangem.features.jointaccount.join.invitepreview.state.transformers.UpdateInvitePreviewInitialStateTransformer
import com.tangem.features.jointaccount.join.invitepreview.state.transformers.UpdateInvitePreviewWalletsTransformer
import com.tangem.features.jointaccount.join.invitepreview.ui.state.JointAccountInvitePreviewUM
import com.tangem.features.jointaccount.join.model.JointAccountJoinChildParams
import com.tangem.features.jointaccount.join.navigation.JointAccountJoinRoute
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class JointAccountInvitePreviewModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val stateController: JointAccountInvitePreviewStateController,
    userWalletsListRepository: UserWalletsListRepository,
    val portfolioSelectorController: PortfolioSelectorController,
    portfolioFetcherFactory: PortfolioFetcher.Factory,
) : Model() {

    private val params = paramsContainer.require<JointAccountJoinChildParams>()

    val portfolioSelectorNavigation = SlotNavigation<Unit>()

    val portfolioFetcher: PortfolioFetcher by lazy {
        portfolioFetcherFactory.create(
            mode = PortfolioFetcher.Mode.All(isOnlyMultiCurrency = false),
            scope = modelScope,
        )
    }

    val portfolioSelectorCallback = object : PortfolioSelectorComponent.BottomSheetCallback {
        override val onDismiss: () -> Unit = { portfolioSelectorNavigation.dismiss() }
        override val onBack: () -> Unit = { portfolioSelectorNavigation.dismiss() }
    }

    /** The wallet picked in the selector, if any */
    private val pickedWallet: StateFlow<UserWallet?> =
        portfolioSelectorController.selectedAccountWithData(portfolioFetcher)
            .map { it?.first }
            .stateIn(modelScope, SharingStarted.Eagerly, null)

    private val unlockedWallets: Flow<List<UserWallet>> = userWalletsListRepository.userWallets
        .map { wallets -> wallets.orEmpty().filterNot(UserWallet::isLocked) }

    /**
     * The wallet that joins the account. A pick in the selector always wins; with no pick yet it defaults to
     * the wallet saved in the draft, then to the wallet selected in the app, then to the first unlocked one.
     */
    private val selectedWallet: StateFlow<UserWallet?> = combine(
        pickedWallet,
        unlockedWallets,
        userWalletsListRepository.selectedUserWallet,
    ) { picked, wallets, currentSelected ->
        val defaultWalletId = params.draftHolder.draft.value.selectedWalletId ?: currentSelected?.walletId

        picked
            ?: wallets.firstOrNull { it.walletId == defaultWalletId }
            ?: wallets.firstOrNull()
    }.stateIn(modelScope, SharingStarted.Eagerly, null)

    val uiState: StateFlow<JointAccountInvitePreviewUM>
        get() = stateController.uiState

    init {
        updateInitialState()
        observeWallets()
        dismissSelectorOnPick()
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

    private fun observeWallets() {
        combine(unlockedWallets, selectedWallet) { wallets, selected ->
            UpdateInvitePreviewWalletsTransformer(
                wallets = wallets,
                selectedWallet = selected,
                onWalletRowClick = ::onWalletRowClick,
            )
        }
            .onEach(stateController::update)
            .launchIn(modelScope)
    }

    /** Closes the wallet selector as soon as the user picks a wallet in it */
    private fun dismissSelectorOnPick() {
        pickedWallet
            .filterNotNull()
            .onEach { portfolioSelectorNavigation.dismiss() }
            .launchIn(modelScope)
    }

    private fun onCreatorInfoClick() {
        // TODO: open the member card of the creator — [REDACTED_TASK_KEY]
    }

    private fun onWalletRowClick() {
        portfolioSelectorNavigation.activate(Unit)
    }

    private fun onContinueClick() {
        val walletId = selectedWallet.value?.walletId ?: return

        params.draftHolder.setSelectedWallet(walletId)
        router.push(JointAccountJoinRoute.DisplayName)
    }

    /** The close icon does not join: the slot stays free and the invitation is not consumed */
    private fun onCloseClick() {
        router.pop()
    }
}