package com.tangem.features.polymarket.impl.entry.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.usecase.GetPolymarketEligibleWalletsUseCase
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioFetcher
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorComponent
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorController
import com.tangem.features.polymarket.api.PolymarketComponent
import com.tangem.features.polymarket.impl.entry.PolymarketEntryBottomSheetConfig
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ModelScoped
@Suppress("LongParameterList")
internal class PolymarketEntryModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val router: Router,
    private val getEligibleWalletsUseCase: GetPolymarketEligibleWalletsUseCase,
    val portfolioSelectorController: PortfolioSelectorController,
    portfolioFetcherFactory: PortfolioFetcher.Factory,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params = paramsContainer.require<PolymarketComponent.Params>()

    val bottomSheetNavigation = SlotNavigation<PolymarketEntryBottomSheetConfig>()

    val portfolioFetcher: PortfolioFetcher by lazy {
        portfolioFetcherFactory.create(
            mode = PortfolioFetcher.Mode.All(isOnlyMultiCurrency = false),
            scope = modelScope,
        )
    }

    val portfolioSelectorCallback = object : PortfolioSelectorComponent.BottomSheetCallback {
        override val onDismiss: () -> Unit = ::dismissSelectorAndPopFeature
        override val onBack: () -> Unit = ::dismissSelectorAndPopFeature
    }

    init {
        portfolioSelectorController.isEnabled.value = { wallet, _ -> getEligibleWalletsUseCase.isEligible(wallet) }
        observeWalletSelection()

        val settledWalletId = params.userWalletId
        if (settledWalletId != null) {
            TangemLogger.i("Entry: caller provided wallet $settledWalletId")
            settle(settledWalletId)
        } else {
            settleFromEligibleWallets()
        }
    }

    private fun settleFromEligibleWallets() {
        modelScope.launch(dispatchers.default) {
            val eligibleWallets = getEligibleWalletsUseCase()
            withContext(dispatchers.mainImmediate) {
                when {
                    eligibleWallets.size == 1 -> {
                        val walletId = eligibleWallets.single().walletId
                        TangemLogger.i("Entry: single eligible wallet $walletId auto-picked")
                        settle(walletId)
                    }
                    eligibleWallets.isEmpty() -> {
                        TangemLogger.i("Entry: no eligible wallets; popping")
                        router.pop()
                    }
                    else -> {
                        TangemLogger.i("Entry: ${eligibleWallets.size} eligible wallets; showing chooser")
                        bottomSheetNavigation.activate(PolymarketEntryBottomSheetConfig.WalletSelector)
                    }
                }
            }
        }
    }

    private fun dismissSelectorAndPopFeature() {
        bottomSheetNavigation.dismiss()
        router.pop()
    }

    private fun observeWalletSelection() {
        portfolioSelectorController.selectedAccountWithData(portfolioFetcher)
            .map { it?.first?.walletId }
            .distinctUntilChanged()
            .filterNotNull()
            .onEach { walletId ->
                TangemLogger.i("Entry: wallet $walletId picked from chooser")
                bottomSheetNavigation.dismiss()
                settle(walletId)
            }
            .launchIn(modelScope)
    }

    /**
     * Still on a coroutine although it no longer awaits anything: `settle` is reached from `init`, and
     * navigating there would run before the bottom-sheet slot is constructed.
     */
    private fun settle(walletId: UserWalletId) {
        modelScope.launch(dispatchers.mainImmediate) {
            TangemLogger.i("Entry: handing over wallet $walletId to onboarding")
            router.replaceAll(PolymarketRoute.Onboarding(userWalletId = walletId))
        }
    }
}