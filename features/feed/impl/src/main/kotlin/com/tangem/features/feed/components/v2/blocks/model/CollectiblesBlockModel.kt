package com.tangem.features.feed.components.v2.blocks.model

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.entity.CollectiblesStartRoute
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.features.collectibles.api.CollectiblesFeatureToggles
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioFetcher
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorComponent
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorController
import com.tangem.features.feed.components.v2.blocks.CollectiblesBlockComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Which wallet the collectibles entry opens on, and on which of its screens.
 *
 * The wallet is settled before the route is pushed: one unlocked wallet is picked silently, several go
 * through the portfolio selector. The picker lives here rather than in the feature because
 * collectibles are a full-screen entry of the root stack, so a sheet raised inside it would scrim an
 * empty screen. An [Account.Collectibles] in the wallet's account list chooses main over onboarding.
 */
@Stable
@ModelScoped
@Suppress("LongParameterList")
internal class CollectiblesBlockModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val singleAccountListSupplier: SingleAccountListSupplier,
    val portfolioSelectorController: PortfolioSelectorController,
    portfolioFetcherFactory: PortfolioFetcher.Factory,
    featureToggles: CollectiblesFeatureToggles,
) : Model() {

    val isVisible: StateFlow<Boolean> = MutableStateFlow(featureToggles.isCollectiblesEnabled)

    /** Read by the block's component, which owns the slot the chooser is shown in. */
    val bottomSheetNavigation = SlotNavigation<CollectiblesBlockComponent.WalletSelectorConfig>()

    /** Lazy: reading every wallet's balances is only worth it once the chooser is actually opened. */
    val portfolioFetcher: PortfolioFetcher by lazy {
        portfolioFetcherFactory.create(
            mode = PortfolioFetcher.Mode.All(isOnlyMultiCurrency = false),
            scope = modelScope,
        )
    }

    val portfolioSelectorCallback = object : PortfolioSelectorComponent.BottomSheetCallback {
        override val onDismiss: () -> Unit = bottomSheetNavigation::dismiss
        override val onBack: () -> Unit = bottomSheetNavigation::dismiss
    }

    private val walletSelectionJob = JobHolder()

    fun onClick() {
        modelScope.launch {
            val eligibleWallets = userWalletsListRepository.userWalletsSync()
                .filter { !it.isLocked && it.isMultiCurrency }

            when {
                eligibleWallets.isEmpty() -> TangemLogger.i("Collectibles: no eligible wallets; staying on the feed")
                eligibleWallets.size == 1 -> openCollectibles(eligibleWallets.single().walletId)
                else -> showWalletSelector()
            }
        }
    }

    private fun showWalletSelector() {
        portfolioSelectorController.isEnabled.value = { wallet, _ -> wallet.isMultiCurrency }
        portfolioSelectorController.selectAccount(accountId = null)
        observeWalletSelection()
        bottomSheetNavigation.activate(CollectiblesBlockComponent.WalletSelectorConfig)
    }

    private fun observeWalletSelection() {
        portfolioSelectorController.selectedAccountWithData(portfolioFetcher)
            .map { it?.first?.walletId }
            .distinctUntilChanged()
            .filterNotNull()
            .onEach { walletId ->
                bottomSheetNavigation.dismiss()
                openCollectibles(walletId)
            }
            .launchIn(modelScope)
            .saveIn(walletSelectionJob)
    }

    private suspend fun openCollectibles(userWalletId: UserWalletId) {
        val accountList = singleAccountListSupplier.getSyncOrNull(userWalletId = userWalletId)
        val hasAccount = accountList?.accounts?.any { it is Account.Collectibles } == true
        val startRoute = if (hasAccount) CollectiblesStartRoute.Main else CollectiblesStartRoute.Onboarding

        router.push(AppRoute.Collectibles(userWalletId = userWalletId, startRoute = startRoute))
    }
}