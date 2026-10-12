package com.tangem.features.feed.components.v2

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.ui.R
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.ds2.button.Back
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.shtorka.ShtorkaExpansion
import com.tangem.core.ui.ds2.topnavigation.TangemNavigationText
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_share_android_20
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.features.feed.components.news.details.DefaultNewsDetailsComponent
import com.tangem.features.feed.nav.FeedRoute
import com.tangem.features.feed.nav.FeedScreenComponent
import com.tangem.features.feed.nav.expandingSheetClick
import com.tangem.features.feed.ui.news.details.state.ArticlesStateUM
import com.tangem.features.feed.ui.v2.FeedNavActionButton
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Stable
internal class FeedNewsDetailsComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: FeedRoute.NewsDetails,
) : FeedScreenComponent, AppComponentContext by context {

    private val articleId = params.articleId

    private val legacyComponent = DefaultNewsDetailsComponent(
        appComponentContext = context.child(key = "legacy"),
        params = DefaultNewsDetailsComponent.Params(
            screenSource = params.source,
            articleId = params.articleId,
            onBackClicked = { router.pop() },
            onTokenClick = ::pushTokenDetails,
            preselectedArticlesId = params.preselectedArticlesId,
            paginationConfig = params.paginationConfig,
        ),
    )

    @Composable
    override fun Header(sheetExpansion: State<ShtorkaExpansion>, onExpandSheet: () -> Unit, modifier: Modifier) {
        val state by legacyComponent.state.collectAsStateWithLifecycle()

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
                    isEnabled = state.articlesStateUM is ArticlesStateUM.Content,
                )
            },
            contentColumn = {
                TangemNavigationText(
                    text = resourceReference(R.string.common_news),
                    role = TangemNavigationText.Role.Title,
                )
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

    private fun pushTokenDetails(token: TokenMarketParams, appCurrency: AppCurrency) {
        router.push(
            route = FeedRoute.MarketsTokenDetails(
                token = token,
                appCurrency = appCurrency,
                shouldShowPortfolio = true,
                analyticsParams = FeedRoute.MarketsTokenDetails.AnalyticsParams(
                    blockchain = null,
                    source = AnalyticsParam.ScreensSources.NewsPage.value,
                    newsId = articleId,
                ),
            ),
        )
    }

    @AssistedFactory
    interface Factory {
        fun create(context: AppComponentContext, params: FeedRoute.NewsDetails): FeedNewsDetailsComponent
    }
}