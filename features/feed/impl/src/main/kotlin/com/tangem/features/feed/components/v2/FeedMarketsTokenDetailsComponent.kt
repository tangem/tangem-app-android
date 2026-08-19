package com.tangem.features.feed.components.v2

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioComponent
import com.tangem.features.commonfeatures.api.managefunds.ManageFundsComponent
import com.tangem.features.feed.components.market.details.DefaultMarketsTokenDetailsComponent
import com.tangem.features.feed.components.market.details.portfolioblock.PortfolioBlockComponent
import com.tangem.features.feed.nav.FeedRoute
import com.tangem.features.feed.nav.FeedScreenComponent
import com.tangem.features.foryou.ForYouFeatureToggles
import com.tangem.features.foryou.TokenSummaryBlockComponent
import com.tangem.features.foryou.TokenSummaryComponent
import com.tangem.features.marketing.api.MarketingBannerComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

/**
 * [FeedRoute.MarketsTokenDetails] screen of the v2 feed: the legacy token details content topped
 * with the DS3 [TangemTopNavigation] instead of the legacy sheet-header title. The legacy Params
 * callbacks are satisfied with router pushes — no lambdas cross the route.
 */
@Stable
@Suppress("LongParameterList")
internal class FeedMarketsTokenDetailsComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: FeedRoute.MarketsTokenDetails,
    analyticsEventHandler: AnalyticsEventHandler,
    portfolioBlockComponentFactory: PortfolioBlockComponent.Factory,
    addToPortfolioComponentFactory: AddToPortfolioComponent.Factory,
    manageFundsComponentFactory: ManageFundsComponent.Factory,
    marketingBannerComponentFactory: MarketingBannerComponent.Factory,
    tokenSummaryBlockComponentFactory: TokenSummaryBlockComponent.Factory,
    forYouFeatureToggles: ForYouFeatureToggles,
) : FeedScreenComponent, AppComponentContext by context {

    private val title = stringReference(params.token.name)
    private val subtitle = stringReference(params.token.symbol)

    private val legacyComponent = DefaultMarketsTokenDetailsComponent(
        appComponentContext = context.child(key = "legacy"),
        analyticsEventHandler = analyticsEventHandler,
        portfolioBlockComponentFactory = portfolioBlockComponentFactory,
        params = DefaultMarketsTokenDetailsComponent.Params(
            token = params.token,
            appCurrency = params.appCurrency,
            shouldShowPortfolio = params.shouldShowPortfolio,
            isTokenSummaryEnabled = forYouFeatureToggles.isForYouEnabled,
            analyticsParams = params.analyticsParams?.let { analyticsParams ->
                DefaultMarketsTokenDetailsComponent.AnalyticsParams(
                    blockchain = analyticsParams.blockchain,
                    source = analyticsParams.source,
                    newsId = analyticsParams.newsId,
                )
            },
            callbacks = routerCallbacks(),
            preselectedSection = params.preselectedSection,
            shouldOpenExchanges = params.shouldOpenExchanges,
            exchangesCount = params.exchangesCount,
        ),
        addToPortfolioComponentFactory = addToPortfolioComponentFactory,
        manageFundsComponentFactory = manageFundsComponentFactory,
        marketingBannerComponentFactory = marketingBannerComponentFactory,
        tokenSummaryBlockComponentFactory = tokenSummaryBlockComponentFactory,
    )

    @Composable
    override fun Header(bottomSheetState: State<BottomSheetState>, onExpandSheet: () -> Unit, modifier: Modifier) {
        TangemTopNavigation(
            title = title,
            subtitle = subtitle,
            contentAlign = TangemTopNavigation.ContentAlign.Center,
            // rendered inside the shtorka's fixed-height sheet header: no system insets and no
            // vertical padding, the host centers the bar vertically
            windowInsets = WindowInsets(0.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            // the sheet header is an opaque capsule (the search bar draws no fade either), and the
            // haze blur re-renders every frame of the header animation — both stay off
            fadeEnabled = false,
            blurBackground = false,
            onBack = { router.pop() },
            modifier = modifier,
        )
    }

    @Composable
    override fun Content(
        bottomSheetState: State<BottomSheetState>,
        contentPadding: PaddingValues,
        onExpandSheet: () -> Unit,
        modifier: Modifier,
    ) {
        legacyComponent.Content(
            bottomSheetState = bottomSheetState,
            contentPadding = contentPadding,
            modifier = modifier,
        )
    }

    private fun routerCallbacks(): DefaultMarketsTokenDetailsComponent.Callbacks {
        return object : DefaultMarketsTokenDetailsComponent.Callbacks {

            override fun onBackClicked() {
                router.pop()
            }

            override fun onArticleClick(articleId: Int, preselectedArticlesId: List<Int>) {
                router.push(
                    route = FeedRoute.NewsDetails(
                        articleId = articleId,
                        source = AnalyticsParam.ScreensSources.Token.value,
                        preselectedArticlesId = preselectedArticlesId,
                    ),
                )
            }

            override fun onTokenSummaryClick(
                userWalletId: UserWalletId,
                token: TokenSummaryComponent.Token,
                selectedTokenPeriodId: String?,
            ) {
                router.push(
                    route = FeedRoute.TokenSummary(
                        token = token.toRouteToken(),
                        selectedTokenPeriodId = selectedTokenPeriodId,
                    ),
                )
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            context: AppComponentContext,
            params: FeedRoute.MarketsTokenDetails,
        ): FeedMarketsTokenDetailsComponent
    }
}

private fun TokenSummaryComponent.Token.toRouteToken(): FeedRoute.TokenSummary.Token {
    return when (this) {
        is TokenSummaryComponent.Token.Portfolio -> FeedRoute.TokenSummary.Token.Portfolio(
            cryptoCurrency = cryptoCurrency,
        )
        is TokenSummaryComponent.Token.Market -> FeedRoute.TokenSummary.Token.Market(
            cryptoCurrencyRawId = cryptoCurrencyRawId,
            symbol = symbol,
            title = title,
            tangemIconUrl = tangemIconUrl,
            networks = networks,
        )
    }
}