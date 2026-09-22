package com.tangem.features.feed.components.v2

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.ds2.button.Back
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.shtorka.ShtorkaExpansion
import com.tangem.core.ui.ds2.topnavigation.TangemNavigationText
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_share_android_20
import com.tangem.core.ui.res.generated.icons.ic_sign_plus_20
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioComponent
import com.tangem.features.commonfeatures.api.managefunds.ManageFundsComponent
import com.tangem.features.feed.components.market.details.DefaultMarketsTokenDetailsComponent
import com.tangem.features.feed.components.market.details.portfolioblock.PortfolioBlockComponent
import com.tangem.features.feed.nav.FeedRoute
import com.tangem.features.feed.nav.FeedScreenComponent
import com.tangem.features.feed.nav.expandingSheetClick
import com.tangem.features.feed.ui.v2.FeedNavActionButton
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
) : FeedScreenComponent, AppComponentContext by context {

    private val title = stringReference(params.token.name)

    private val legacyComponent = DefaultMarketsTokenDetailsComponent(
        appComponentContext = context.child(key = "legacy"),
        analyticsEventHandler = analyticsEventHandler,
        portfolioBlockComponentFactory = portfolioBlockComponentFactory,
        params = DefaultMarketsTokenDetailsComponent.Params(
            token = params.token,
            appCurrency = params.appCurrency,
            shouldShowPortfolio = params.shouldShowPortfolio,
            isTokenSummaryEnabled = true,
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
    override fun Header(sheetExpansion: State<ShtorkaExpansion>, onExpandSheet: () -> Unit, modifier: Modifier) {
        val state by legacyComponent.state.collectAsStateWithLifecycle()
        val isPriceShown = state.shouldShowPriceSubtitle || sheetExpansion.value == ShtorkaExpansion.PILL

        TangemTopNavigation(
            contentAlign = TangemTopNavigation.ContentAlign.Center,
            windowInsets = WindowInsets(0.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            fadeEnabled = false,
            blurBackground = false,
            isEndButtonsGroupBackgroundShown = false,
            startButton = {
                TangemButton.Back(onClick = expandingSheetClick(sheetExpansion, onExpandSheet) { router.pop() })
            },
            endButtonsGroup = {
                FeedNavActionButton(
                    icon = Icons.ic_share_android_20,
                    onClick = expandingSheetClick(sheetExpansion, onExpandSheet, state.onShareClick),
                )
            },
            endButton = if (state.isAddToPortfolioButtonVisible) {
                {
                    FeedNavActionButton(
                        icon = Icons.ic_sign_plus_20,
                        onClick = expandingSheetClick(sheetExpansion, onExpandSheet, state.onAddToPortfolioClick),
                        // added to every account: the tap explains that instead of doing nothing
                        isDimmed = !state.isAddToPortfolioButtonEnabled,
                    )
                }
            } else {
                null
            },
            contentColumn = {
                TangemNavigationText(text = title, role = TangemNavigationText.Role.Title)
                AnimatedVisibility(visible = isPriceShown) {
                    Column {
                        Spacer(Modifier.height(4.dp))
                        TangemNavigationText(
                            text = stringReference(state.priceText),
                            role = TangemNavigationText.Role.Subtitle,
                        )
                    }
                }
            },
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