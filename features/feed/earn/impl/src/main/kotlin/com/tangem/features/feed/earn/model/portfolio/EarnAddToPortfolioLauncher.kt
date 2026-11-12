package com.tangem.features.feed.earn.model.portfolio

import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.markets.RawMarketToken
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.earn.EarnTokenWithCurrency
import com.tangem.domain.models.earn.EarnType
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.GetStakingAvailabilityUseCase
import com.tangem.domain.staking.model.optionOrNull
import com.tangem.domain.yield.supply.models.YieldSupplyAvailability
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetAvailabilityUseCase
import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioManager
import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioManager.AnalyticsParams.Companion.CategoryEarn
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioFetcher
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorBridge
import com.tangem.features.earn.api.EarnFeatureToggles
import com.tangem.features.feed.earn.model.analytics.EarnAnalyticsEvent
import com.tangem.features.feed.earn.model.analytics.EarnSource
import com.tangem.utils.annotations.RemoveWithToggle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.job

/**
 * Opens the "add to portfolio" sheet for an earn opportunity. Shared by the Earn feed tab and its
 * search page, which offer the same rows and must therefore behave identically on a tap.
 *
 * The sheet is shown through [showSheet]/[dismissSheet] rather than through a slot navigation, because
 * the two hosts model different sets of sheets: the tab also has the two filter sheets, the search page
 * has only this one.
 *
 * @param showSheet presents the sheet for a freshly built manager
 * @param dismissSheet closes whatever the host currently shows
 */
@Suppress("LongParameterList")
internal class EarnAddToPortfolioLauncher(
    private val addToPortfolioManagerFactory: AddToPortfolioManager.Factory,
    private val portfolioSelectorBridgeFactory: PortfolioSelectorBridge.Factory,
    private val earnFeatureToggles: EarnFeatureToggles,
    private val getStakingAvailabilityUseCase: GetStakingAvailabilityUseCase,
    private val yieldSupplyGetAvailabilityUseCase: YieldSupplyGetAvailabilityUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val router: Router,
    private val modelScope: CoroutineScope,
    private val showSheet: (source: EarnSource, manager: AddToPortfolioManager) -> Unit,
    private val dismissSheet: () -> Unit,
) {

    private var currentManagerScope: CoroutineScope? = null

    fun launch(earnTokenWithCurrency: EarnTokenWithCurrency, source: EarnSource) {
        analyticsEventHandler.send(
            EarnAnalyticsEvent.OpportunitySelected(
                tokenSymbol = earnTokenWithCurrency.earnToken.tokenSymbol,
                blockchain = earnTokenWithCurrency.cryptoCurrency.network.name,
                source = source.value,
            ),
        )
        val token = RawMarketToken(
            id = CryptoCurrency.RawID(earnTokenWithCurrency.earnToken.tokenId),
            name = earnTokenWithCurrency.earnToken.tokenName,
            symbol = earnTokenWithCurrency.earnToken.tokenSymbol,
        )
        val network = TokenMarketInfo.Network(
            networkId = earnTokenWithCurrency.earnToken.networkId,
            isExchangeable = false,
            contractAddress = earnTokenWithCurrency.earnToken.tokenAddress,
            decimalCount = earnTokenWithCurrency.earnToken.decimalCount,
        )
        val manager = createManager(source = source, earnType = earnTokenWithCurrency.earnToken.type).apply {
            setTokenParams(token)
            setTokenNetworks(listOf(network))
        }
        // Drop the slot through null so the same-source repeat click still recreates the child.
        dismissSheet()
        showSheet(source, manager)
    }

    private fun createManager(source: EarnSource, earnType: EarnType): AddToPortfolioManager {
        currentManagerScope?.cancel()
        val managerScope = CoroutineScope(
            modelScope.coroutineContext + SupervisorJob(modelScope.coroutineContext.job),
        )
        currentManagerScope = managerScope

        val manager = addToPortfolioManagerFactory.create(
            scope = managerScope,
            settings = AddToPortfolioManager.Settings.Earn,
            analyticsParams = AddToPortfolioManager.AnalyticsParams(source = source.value, category = CategoryEarn),
            portfolioSelectorBridge = portfolioSelectorBridgeFactory.create(
                mode = PortfolioFetcher.Mode.All(isOnlyMultiCurrency = true),
                scope = managerScope,
                // Neither staking nor Yield Mode can work with a Safe-held balance
                isJointAccountsIncluded = false,
            ),
        ).apply {
            updateLaunchMode(launchMode())
        }

        manager.onDismiss.receiveAsFlow()
            .onEach { dismissSheet() }
            .launchIn(managerScope)
        manager.onSuccessAdded.receiveAsFlow()
            .onEach { dismissSheet() }
            .onEach { result -> router.push(earnRoute(earnType, result)) }
            .launchIn(managerScope)
        manager.onAddedTokenClick.receiveAsFlow()
            .onEach { dismissSheet() }
            .onEach { result -> router.push(earnRoute(earnType, result)) }
            .launchIn(managerScope)

        return manager
    }

    @RemoveWithToggle("AND_18953_EARN_UPDATE_ASSET_SELECTION", description = "Inline ViaUserPortfolio")
    private fun launchMode(): AddToPortfolioManager.LaunchMode = if (earnFeatureToggles.isUserPortfolioEnabled) {
        AddToPortfolioManager.LaunchMode.ViaUserPortfolio(shouldScopeToNetworks = true)
    } else {
        AddToPortfolioManager.LaunchMode.Preselected
    }

    /**
     * [EarnTokenWithCurrency.cryptoCurrency] is only fit for the icon, so the route is built from the currency the
     * sheet actually added, which carries the chosen account's derivation. The earn list does not know whether the
     * chosen account can stake or enter Yield Mode (joint accounts cannot), so availability is asked for that
     * currency, and its APY is used because the earn list carries a fraction while Yield Mode expects a percent.
     */
    @RemoveWithToggle("AND_18953_EARN_UPDATE_ASSET_SELECTION", description = "Drop the CurrencyDetails-only branch")
    private suspend fun earnRoute(earnType: EarnType, result: AddToPortfolioManager.Result): AppRoute {
        val userWalletId = result.wallet.walletId
        val currency = result.addedCurrency.currency
        val earnRoute = if (earnFeatureToggles.isUserPortfolioEnabled) {
            when (earnType) {
                EarnType.STAKING -> stakingRoute(userWalletId, currency)
                EarnType.YIELD -> yieldRoute(userWalletId, currency)
            }
        } else {
            null
        }
        return earnRoute ?: AppRoute.CurrencyDetails(userWalletId = userWalletId, currency = currency)
    }

    private suspend fun stakingRoute(userWalletId: UserWalletId, currency: CryptoCurrency): AppRoute? {
        val option = getStakingAvailabilityUseCase.invokeSync(userWalletId, currency).getOrNull()?.optionOrNull
            ?: return null
        return AppRoute.Staking(
            userWalletId = userWalletId,
            cryptoCurrency = currency,
            integrationId = option.integrationId,
        )
    }

    private suspend fun yieldRoute(userWalletId: UserWalletId, currency: CryptoCurrency): AppRoute? {
        val available = yieldSupplyGetAvailabilityUseCase(currency).getOrNull() as? YieldSupplyAvailability.Available
            ?: return null
        return AppRoute.YieldSupplyEntry(userWalletId = userWalletId, cryptoCurrency = currency, apy = available.apy)
    }
}