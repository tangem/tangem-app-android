package com.tangem.features.feed.earn.model.portfolio

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.markets.RawMarketToken
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.earn.EarnTokenWithCurrency
import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioManager
import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioManager.AnalyticsParams.Companion.CategoryEarn
import com.tangem.features.feed.earn.model.analytics.EarnAnalyticsEvent
import com.tangem.features.feed.earn.model.analytics.EarnSource
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
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val appRouter: AppRouter,
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
        val manager = createManager(source = source).apply {
            setTokenParams(token)
            setTokenNetworks(listOf(network))
        }
        // Drop the slot through null so the same-source repeat click still recreates the child.
        dismissSheet()
        showSheet(source, manager)
    }

    private fun createManager(source: EarnSource): AddToPortfolioManager {
        currentManagerScope?.cancel()
        val managerScope = CoroutineScope(
            modelScope.coroutineContext + SupervisorJob(modelScope.coroutineContext.job),
        )
        currentManagerScope = managerScope

        val manager = addToPortfolioManagerFactory.create(
            scope = managerScope,
            settings = AddToPortfolioManager.Settings.Earn,
            analyticsParams = AddToPortfolioManager.AnalyticsParams(source = source.value, category = CategoryEarn),
        ).apply {
            updateLaunchMode(AddToPortfolioManager.LaunchMode.Preselected)
        }

        manager.onDismiss.receiveAsFlow()
            .onEach { dismissSheet() }
            .launchIn(managerScope)
        manager.onSuccessAdded.receiveAsFlow()
            .onEach { dismissSheet() }
            .onEach(::openCurrencyDetails)
            .launchIn(managerScope)
        manager.onAddedTokenClick.receiveAsFlow()
            .onEach { dismissSheet() }
            .onEach(::openCurrencyDetails)
            .launchIn(managerScope)

        return manager
    }

    private fun openCurrencyDetails(result: AddToPortfolioManager.Result) {
        appRouter.push(
            AppRoute.CurrencyDetails(
                userWalletId = result.wallet.walletId,
                currency = result.addedCurrency.currency,
            ),
        )
    }
}