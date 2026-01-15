package com.tangem.features.feed.components.market.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.blockchainsdk.compatibility.getTokenIdIfL2Network
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.decompose.ComposableModularBottomSheetContentComponent
import com.tangem.core.ui.res.LocalMainBottomSheetColor
import com.tangem.domain.account.featuretoggle.AccountsFeatureToggles
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.feed.components.market.details.portfolio.api.MarketsPortfolioComponent
import com.tangem.features.feed.model.market.details.MarketsTokenDetailsModel
import com.tangem.features.feed.model.market.details.analytics.MarketDetailsAnalyticsEvent
import com.tangem.features.feed.model.market.details.state.TokenNetworksState
import com.tangem.features.feed.ui.market.detailed.MarketsTokenDetailsContent
import com.tangem.features.feed.ui.market.detailed.MarketsTokenDetailsTopBar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

internal class DefaultMarketsTokenDetailsComponent(
    appComponentContext: AppComponentContext,
    val params: Params,
    analyticsEventHandler: AnalyticsEventHandler,
    private val accountsFeatureToggles: AccountsFeatureToggles,
    portfolioComponentFactory: MarketsPortfolioComponent.Factory,
) : ComposableModularBottomSheetContentComponent, AppComponentContext by appComponentContext {

    // applying l2 compatibility
    private val updatedParams = params.copy(
        token = params.token.copy(
            id = CryptoCurrency.RawID(getTokenIdIfL2Network(params.token.id.value)),
        ),
    )
    private val analyticsParams = params.analyticsParams
    private val model: MarketsTokenDetailsModel = getOrCreateModel(updatedParams)

    private val portfolioComponent: MarketsPortfolioComponent? = if (updatedParams.shouldShowPortfolio) {
        portfolioComponentFactory.create(
            context = child("my_portfolio"),
            params = MarketsPortfolioComponent.Params(
                updatedParams.token,
                analyticsParams = analyticsParams?.source?.let { MarketsPortfolioComponent.AnalyticsParams(it) },
            ),
        )
    } else {
        null
    }

    init {

        componentScope.launch(dispatchers.default) {
            model.networksState.collectLatest { state ->
                when (state) {
                    is TokenNetworksState.NetworksAvailable -> portfolioComponent?.setTokenNetworks(state.networks)
                    TokenNetworksState.NoNetworksAvailable -> portfolioComponent?.setNoNetworksAvailable()
                    else -> {}
                }
            }
        }

        // === Analytics ===
        if (analyticsParams != null) {
            analyticsEventHandler.send(
                MarketDetailsAnalyticsEvent.EventBuilder(
                    token = params.token,
                ).screenOpened(
                    blockchain = analyticsParams.blockchain,
                    source = analyticsParams.source,
                    newsId = analyticsParams.newsId,
                ),
            )
        }
    }

    @Composable
    override fun Title(bottomSheetState: State<BottomSheetState>) {
        val state by model.state.collectAsStateWithLifecycle()
        val background = LocalMainBottomSheetColor.current.value
        MarketsTokenDetailsTopBar(
            onBackClick = { params.onBackClicked() },
            isBackButtonEnabled = bottomSheetState.value == BottomSheetState.EXPANDED,
            shouldShowPriceSubtitle = state.shouldShowPriceSubtitle,
            tokenName = state.tokenName,
            tokenPrice = state.priceText,
            backgroundColor = background,
        )
    }

    @Composable
    override fun Content(bottomSheetState: State<BottomSheetState>, modifier: Modifier) {
        LifecycleStartEffect(Unit) {
            model.isVisibleOnScreen.value = true
            onStopOrDispose {
                model.isVisibleOnScreen.value = false
            }
        }
        val state by model.state.collectAsStateWithLifecycle()
        val bsState by bottomSheetState
        LaunchedEffect(bsState) {
            model.isVisibleOnScreen.value = bsState == BottomSheetState.EXPANDED
        }

        MarketsTokenDetailsContent(
            modifier = modifier,
            backgroundColor = LocalMainBottomSheetColor.current.value,
            state = state,
            isAccountEnabled = accountsFeatureToggles.isFeatureEnabled,
            portfolioBlock = portfolioComponent?.let { component ->
                { blockModifier ->
                    component.Content(blockModifier)
                }
            },
        )
    }

    @Serializable
    data class Params(
        val token: TokenMarketParams,
        val appCurrency: AppCurrency,
        val shouldShowPortfolio: Boolean,
        val analyticsParams: AnalyticsParams?,
        val onBackClicked: () -> Unit,
        val onArticleClick: (articleId: Int, preselectedArticlesId: List<Int>) -> Unit,
    )

    @Serializable
    data class AnalyticsParams(
        val blockchain: String?,
        val source: String,
        val newsId: Int? = null,
    )
}