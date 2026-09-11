package com.tangem.features.feed.model.v2

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
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioFetcher
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorComponent
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorController
import com.tangem.features.feed.components.v2.FeedTopBlocksBottomSheetConfig
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
@Suppress("LongParameterList")
internal class FeedTopBlocksModel @Inject constructor(
    private val router: Router,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val singleAccountListSupplier: SingleAccountListSupplier,
    val portfolioSelectorController: PortfolioSelectorController,
    portfolioFetcherFactory: PortfolioFetcher.Factory,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    val bottomSheetNavigation = SlotNavigation<FeedTopBlocksBottomSheetConfig>()

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

    fun onCollectiblesClick() {
        modelScope.launch {
            val eligibleWallets = userWalletsListRepository.userWalletsSync().filterNot { it.isLocked }

            when {
                eligibleWallets.isEmpty() -> TangemLogger.i("Collectibles: no unlocked wallets; staying on the feed")
                eligibleWallets.size == 1 -> openCollectibles(eligibleWallets.single().walletId)
                else -> showWalletSelector()
            }
        }
    }

    private fun showWalletSelector() {
        portfolioSelectorController.selectAccount(accountId = null)
        observeWalletSelection()
        bottomSheetNavigation.activate(FeedTopBlocksBottomSheetConfig.WalletSelector)
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