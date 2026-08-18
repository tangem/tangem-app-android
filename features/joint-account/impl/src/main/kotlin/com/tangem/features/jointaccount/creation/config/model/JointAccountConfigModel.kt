package com.tangem.features.jointaccount.creation.config.model

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.isLocked
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioFetcher
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorComponent
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorController
import com.tangem.features.jointaccount.creation.config.state.JointAccountConfigStateController
import com.tangem.features.jointaccount.creation.config.state.transformers.*
import com.tangem.features.jointaccount.creation.config.ui.state.JointAccountConfigUM
import com.tangem.features.jointaccount.creation.model.JointAccountCreationChildParams
import com.tangem.features.jointaccount.creation.model.JointAccountCreationDraft
import com.tangem.features.jointaccount.creation.navigation.JointAccountCreationRoute
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
internal class JointAccountConfigModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val stateController: JointAccountConfigStateController,
    userWalletsListRepository: UserWalletsListRepository,
    val portfolioSelectorController: PortfolioSelectorController,
    portfolioFetcherFactory: PortfolioFetcher.Factory,
) : Model() {

    private val params = paramsContainer.require<JointAccountCreationChildParams>()

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

     * to the wallet saved in the draft, then to the wallet the flow was opened from.
     */
    private val selectedWallet: StateFlow<UserWallet?> = combine(
        pickedWallet,
        unlockedWallets,
    ) { picked, wallets ->
        val draftConfig = params.draftHolder.draft.value.config
        val defaultWalletId = draftConfig?.walletId ?: params.userWalletId

        picked
            ?: wallets.firstOrNull { it.walletId == defaultWalletId }
            ?: wallets.firstOrNull()
    }.stateIn(modelScope, SharingStarted.Eagerly, null)

    val uiState: StateFlow<JointAccountConfigUM>
        get() = stateController.uiState

    init {
        updateInitialState()
        restoreDraft()
        observeWallets()
        dismissSelectorOnPick()
    }

    private fun updateInitialState() {
        stateController.update(
            UpdateConfigInitialStateTransformer(
                onNameChange = ::onNameChange,
                onColorClick = ::onColorClick,
                onIconClick = ::onIconClick,
                onContinueClick = ::onContinueClick,
                onBackClick = ::onBackClick,
            ),
        )
    }

    private fun restoreDraft() {
        val config = params.draftHolder.draft.value.config ?: return

        stateController.update(RestoreConfigDraftTransformer(draft = config))
    }

    private fun observeWallets() {
        combine(unlockedWallets, selectedWallet) { wallets, selected ->
            UpdateWalletsTransformer(
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

    private fun onNameChange(name: String) {
        stateController.update(UpdateAccountNameTransformer(name = name))
    }

    private fun onColorClick(color: CryptoPortfolioIcon.Color) {
        stateController.update(SelectColorTransformer(color = color))
    }

    private fun onIconClick(icon: CryptoPortfolioIcon.Icon) {
        stateController.update(SelectIconTransformer(icon = icon))
    }

    private fun onWalletRowClick() {
        portfolioSelectorNavigation.activate(Unit)
    }

    private fun onContinueClick() {
        val state = uiState.value

        params.draftHolder.setConfig(
            JointAccountCreationDraft.Config(
                name = state.name,
                icon = state.icon.value,
                color = state.icon.color,
                walletId = selectedWallet.value?.walletId ?: params.userWalletId,
            ),
        )

        router.push(JointAccountCreationRoute.Composition)
    }

    /** Back returns to the promo step: the flow's inner router pops its own stack first */
    private fun onBackClick() {
        router.pop()
    }
}