package com.tangem.features.polymarket.impl.entry.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.markets.RawMarketToken
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isLocked
import com.tangem.blockchainsdk.utils.toCoinId
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.domain.polymarket.PolymarketDepositBlockchain
import com.tangem.domain.polymarket.usecase.GetPolymarketEligibleWalletsUseCase
import com.tangem.domain.polymarket.usecase.HasPolymarketDepositNetworkUseCase
import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioManager
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioFetcher
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorBridge
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorComponent
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorController
import com.tangem.features.polymarket.api.PolymarketComponent
import com.tangem.features.polymarket.impl.entry.PolymarketEntryBottomSheetConfig
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ModelScoped
@Suppress("LongParameterList")
internal class PolymarketEntryModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val router: Router,
    private val getEligibleWalletsUseCase: GetPolymarketEligibleWalletsUseCase,
    private val hasDepositNetworkUseCase: HasPolymarketDepositNetworkUseCase,
    private val addToPortfolioManagerFactory: AddToPortfolioManager.Factory,
    private val portfolioSelectorBridgeFactory: PortfolioSelectorBridge.Factory,
    val portfolioSelectorController: PortfolioSelectorController,
    portfolioFetcherFactory: PortfolioFetcher.Factory,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params = paramsContainer.require<PolymarketComponent.Params>()

    val bottomSheetNavigation = SlotNavigation<PolymarketEntryBottomSheetConfig>()

    var addToPortfolioManager: AddToPortfolioManager? = null
        private set

    private var addToPortfolioManagerScope: CoroutineScope? = null

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
        portfolioSelectorController.isEnabled.value = { wallet, _ -> !wallet.isLocked }
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

    private fun settle(walletId: UserWalletId) {
        modelScope.launch(dispatchers.default) {
            val hasDepositNetwork = hasDepositNetworkUseCase(walletId)
            TangemLogger.i("Entry: wallet $walletId hasDepositNetwork=$hasDepositNetwork")
            withContext(dispatchers.mainImmediate) {
                if (hasDepositNetwork) {
                    handOver(walletId)
                } else {
                    offerToAddDepositNetwork(walletId)
                }
            }
        }
    }

    private fun handOver(walletId: UserWalletId) {
        TangemLogger.i("Entry: handing over wallet $walletId to onboarding")
        router.replaceAll(PolymarketRoute.Onboarding(userWalletId = walletId))
    }

    private fun offerToAddDepositNetwork(walletId: UserWalletId) {
        TangemLogger.i("Entry: opening add-deposit-network sheet for wallet $walletId")
        addToPortfolioManager = createAddToPortfolioManager(walletId)
        bottomSheetNavigation.activate(PolymarketEntryBottomSheetConfig.AddDepositNetwork)
    }

    /**
     * The sheet is bridged to [walletId] alone. Predictions has already settled on that wallet, and a sheet
     * that offered the others could complete an add on a wallet this model is not waiting for — which it would
     * then have to refuse, leaving the user on a flow that cannot go forward. Loading one portfolio also means
     * that a wallet with a single account has nothing left to choose, so the selector is skipped outright.
     *
     * The bridge lives on the manager's scope, which is cancelled and rebuilt per offer.
     */
    private fun createAddToPortfolioManager(walletId: UserWalletId): AddToPortfolioManager {
        addToPortfolioManagerScope?.cancel()
        val managerScope = CoroutineScope(modelScope.coroutineContext + SupervisorJob(modelScope.coroutineContext.job))
        addToPortfolioManagerScope = managerScope

        val manager = addToPortfolioManagerFactory.create(
            scope = managerScope,
            settings = AddToPortfolioManager.Settings(shouldSkipTokenActionsScreen = true),
            analyticsParams = AddToPortfolioManager.AnalyticsParams(source = null),
            portfolioSelectorBridge = portfolioSelectorBridgeFactory.create(
                mode = PortfolioFetcher.Mode.Wallet(walletId),
                scope = managerScope,
            ),
        ).apply {
            updateLaunchMode(AddToPortfolioManager.LaunchMode.Preselected)
            setTokenParams(depositCoin)
            setTokenNetworks(listOf(depositNetwork))
        }

        manager.onDismiss.receiveAsFlow()
            .onEach { bottomSheetNavigation.dismiss() }
            .launchIn(managerScope)

        merge(manager.onSuccessAdded.receiveAsFlow(), manager.onAddedTokenClick.receiveAsFlow())
            .onEach { result -> onDepositNetworkAdded(settledWalletId = walletId, result = result) }
            .launchIn(managerScope)

        return manager
    }

    private fun onDepositNetworkAdded(settledWalletId: UserWalletId, result: AddToPortfolioManager.Result) {
        bottomSheetNavigation.dismiss()
        if (result.wallet.walletId == settledWalletId) {
            handOver(settledWalletId)
        } else {
            TangemLogger.e(
                "Entry: AddToPortfolio reported success for ${result.wallet.walletId}, " +
                    "but the settled wallet is $settledWalletId; not handing over",
            )
        }
    }

    private companion object {

        /**
         * The deposit chain's own coin. Adding it is the cheapest way to put the chain in a portfolio: the
         * add flow builds a coin rather than a token whenever [TokenMarketInfo.Network.contractAddress] is
         * null, so nothing here has to name a contract or a decimal count.
         */
        val depositCoin = RawMarketToken(
            id = CryptoCurrency.RawID(PolymarketDepositBlockchain.toCoinId()),
            name = PolymarketDepositBlockchain.fullName,
            symbol = PolymarketDepositBlockchain.currency,
        )

        val depositNetwork = TokenMarketInfo.Network(
            networkId = PolymarketDepositBlockchain.toNetworkId(),
            isExchangeable = false,
            contractAddress = null,
            decimalCount = null,
        )
    }
}