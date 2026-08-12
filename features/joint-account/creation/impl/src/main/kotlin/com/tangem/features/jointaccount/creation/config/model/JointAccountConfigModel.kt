package com.tangem.features.jointaccount.creation.config.model

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
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isLocked
import com.tangem.features.jointaccount.creation.component.JointAccountCreationComponent
import com.tangem.features.jointaccount.creation.config.state.JointAccountConfigStateController
import com.tangem.features.jointaccount.creation.config.state.transformers.SelectColorTransformer
import com.tangem.features.jointaccount.creation.config.state.transformers.SelectIconTransformer
import com.tangem.features.jointaccount.creation.config.state.transformers.UpdateAccountNameTransformer
import com.tangem.features.jointaccount.creation.config.state.transformers.UpdateConfigInitialStateTransformer
import com.tangem.features.jointaccount.creation.config.state.transformers.UpdateWalletsTransformer
import com.tangem.features.jointaccount.creation.config.ui.state.JointAccountConfigUM
import com.tangem.features.wallet.utils.UserWalletImageFetcher
import com.tangem.operations.attestation.ArtworkSize
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class JointAccountConfigModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val stateController: JointAccountConfigStateController,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val getWalletTotalBalanceUseCase: GetWalletTotalBalanceUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val userWalletImageFetcher: UserWalletImageFetcher,
) : Model() {

    private val params = paramsContainer.require<JointAccountCreationComponent.Params>()

    private val selectedWalletId = MutableStateFlow(value = params.userWalletId)

    private val isChooseWalletShown = MutableStateFlow(value = false)

    val uiState: StateFlow<JointAccountConfigUM>
        get() = stateController.uiState

    init {
        updateInitialState()
        observeWallets()
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

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeWallets() {
        combine(
            flow = userWalletsListRepository.userWallets.map { wallets ->
                wallets.orEmpty().filterNot(UserWallet::isLocked)
            },
            flow2 = selectedWalletId,
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

    private fun createWalletsTransformer(
        state: WalletsState,
        balances: Map<UserWalletId, TotalFiatBalance>,
        images: Map<UserWalletId, UserWalletItemUM.ImageState>,
    ): UpdateWalletsTransformer = UpdateWalletsTransformer(
        walletsInfo = UpdateWalletsTransformer.WalletsInfo(
            wallets = state.wallets,
            selectedWalletId = state.selectedId,
            isSheetShown = state.isSheetShown,
            balances = balances,
            images = images,
            appCurrency = state.appCurrency,
            isBalanceHidden = state.isBalanceHidden,
        ),
        intents = UpdateWalletsTransformer.Intents(
            onWalletSelect = ::onWalletSelect,
            onWalletRowClick = ::onWalletRowClick,
            onChooseWalletDismiss = ::onChooseWalletDismiss,
        ),
    )

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
        // TODO([REDACTED_TASK_KEY]): open the composition step once it exists
    }

    /** Back returns to the promo step: the flow's inner router pops its own stack first */
    private fun onBackClick() {
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