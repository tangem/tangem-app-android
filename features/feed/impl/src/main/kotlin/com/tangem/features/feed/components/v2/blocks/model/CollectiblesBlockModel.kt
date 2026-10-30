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
import com.tangem.domain.collectibles.account.flow.CollectiblesAccountStatusSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.account.CollectiblesAccountStatusValue
import com.tangem.domain.models.wallet.UserWallet
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Which wallet the collectibles entry opens on, and on which of its screens.
 *
 * The wallet is settled before the route is pushed: one unlocked wallet is picked silently, several go
 * through the portfolio selector. The picker lives here rather than in the feature because
 * collectibles are a full-screen entry of the root stack, so a sheet raised inside it would scrim an
 * empty screen. A wallet whose account is not onboarded opens onboarding, any other opens main.
 *
 * A wallet whose collectibles account waits for the address sync ([CollectiblesAccountStatusValue.MissedDerivation])
 * cannot be entered: the owner key is not on this device yet. The block hides when every wallet is in that state,
 * and the chooser shows such wallets disabled.
 */
@Stable
@ModelScoped
@Suppress("LongParameterList")
internal class CollectiblesBlockModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val collectiblesAccountStatusSupplier: CollectiblesAccountStatusSupplier,
    val portfolioSelectorController: PortfolioSelectorController,
    portfolioFetcherFactory: PortfolioFetcher.Factory,
    featureToggles: CollectiblesFeatureToggles,
) : Model() {

    val isVisible: StateFlow<Boolean> = if (featureToggles.isCollectiblesEnabled) {
        observeEnterableWallet()
    } else {
        MutableStateFlow(false)
    }

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
            val eligibleWallets = userWalletsListRepository.userWalletsSync().filter(::isEligible)
            val enterableWallets = eligibleWallets.filterNot { wallet -> missedDerivation(wallet.walletId) }

            when {
                enterableWallets.isEmpty() -> TangemLogger.i("Collectibles: no enterable wallets; staying on the feed")
                eligibleWallets.size == 1 -> openCollectibles(enterableWallets.single().walletId)
                else -> showWalletSelector(enterable = enterableWallets.mapTo(hashSetOf(), UserWallet::walletId))
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeEnterableWallet(): StateFlow<Boolean> = userWalletsListRepository.userWallets
        .map { wallets -> wallets.orEmpty().filter(::isEligible).map(UserWallet::walletId) }
        .distinctUntilChanged()
        .flatMapLatest { walletIds ->
            if (walletIds.isEmpty()) return@flatMapLatest flowOf(true)

            combine(walletIds.map(collectiblesAccountStatusSupplier::invoke)) { statuses ->
                statuses.any { it !is CollectiblesAccountStatusValue.MissedDerivation }
            }
        }
        .distinctUntilChanged()
        .stateIn(scope = modelScope, started = SharingStarted.Eagerly, initialValue = true)

    private fun isEligible(wallet: UserWallet): Boolean = !wallet.isLocked && wallet.isMultiCurrency

    private suspend fun missedDerivation(userWalletId: UserWalletId): Boolean {
        val status = collectiblesAccountStatusSupplier(userWalletId).first()

        return status is CollectiblesAccountStatusValue.MissedDerivation
    }

    private fun showWalletSelector(enterable: Set<UserWalletId>) {
        portfolioSelectorController.isEnabled.value = { wallet, _ -> wallet.walletId in enterable }
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
        val status = collectiblesAccountStatusSupplier(userWalletId).first()
        val startRoute = if (status is CollectiblesAccountStatusValue.NotOnboarded) {
            CollectiblesStartRoute.Onboarding
        } else {
            CollectiblesStartRoute.Main
        }

        router.push(AppRoute.Collectibles(userWalletId = userWalletId, startRoute = startRoute))
    }
}