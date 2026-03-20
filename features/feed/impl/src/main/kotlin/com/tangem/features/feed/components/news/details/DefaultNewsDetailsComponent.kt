package com.tangem.features.feed.components.news.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.R
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.decompose.ComposableModularBottomSheetContentComponent
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds.topbar.TangemTopBarType
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.LocalMainBottomSheetColor
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.domain.news.model.NewsListConfig
import com.tangem.features.feed.model.news.details.NewsDetailsModel
import com.tangem.features.feed.ui.news.details.NewsDetailsContent
import com.tangem.features.feed.ui.news.details.state.ArticlesStateUM
import kotlinx.serialization.Serializable

internal class DefaultNewsDetailsComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableModularBottomSheetContentComponent, AppComponentContext by appComponentContext {

    private val newsDetailsModel = getOrCreateModel<NewsDetailsModel, Params>(params = params)

    @Composable
    override fun Title(bottomSheetState: State<BottomSheetState>) {
        val background = LocalMainBottomSheetColor.current.value
        val state by newsDetailsModel.state.collectAsStateWithLifecycle()
        if (LocalRedesignEnabled.current) {
            TangemTopBar(
                title = resourceReference(R.string.common_news),
                type = TangemTopBarType.BottomSheet,
                startContent = {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_arrow_back_28),
                        contentDescription = null,
                        tint = TangemTheme.colors2.graphic.neutral.primary,
                        modifier = Modifier
                            .size(TangemTheme.dimens2.x11)
                            .background(
                                color = TangemTheme.colors2.button.backgroundSecondary,
                                shape = CircleShape,
                            )
                            .clickableSingle(
                                onClick = state.onBackClick,
                                enabled = bottomSheetState.value == BottomSheetState.EXPANDED,
                            )
                            .padding(TangemTheme.dimens2.x2),
                    )
                },
                endContent = {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_share_new_24),
                        contentDescription = null,
                        tint = TangemTheme.colors2.graphic.neutral.primary,
                        modifier = Modifier
                            .size(TangemTheme.dimens2.x11)
                            .background(
                                color = TangemTheme.colors2.button.backgroundSecondary,
                                shape = CircleShape,
                            )
                            .clickableSingle(
                                onClick = state.onShareClick,
                                enabled = bottomSheetState.value == BottomSheetState.EXPANDED &&
                                    state.articlesStateUM is ArticlesStateUM.Content,
                            )
                            .padding(TangemTheme.dimens2.x2_5),
                    )
                },
            )
        } else {
            TangemTopAppBar(
                containerColor = background,
                title = null,
                startButton = TopAppBarButtonUM.Icon(
                    iconRes = R.drawable.ic_back_24,
                    onClicked = state.onBackClick,
                    isEnabled = bottomSheetState.value == BottomSheetState.EXPANDED,
                ),
                endButton = TopAppBarButtonUM.Icon(
                    iconRes = R.drawable.ic_share_24,
                    onClicked = state.onShareClick,
                    isEnabled = bottomSheetState.value == BottomSheetState.EXPANDED &&
                        state.articlesStateUM is ArticlesStateUM.Content,
                ),
            )
        }
    }

    @Composable
    override fun Content(bottomSheetState: State<BottomSheetState>, modifier: Modifier) {
        val state by newsDetailsModel.state.collectAsStateWithLifecycle()
        NewsDetailsContent(
            state = state,
            modifier = modifier,
        )
    }

    @Serializable
    data class Params(
        val screenSource: String,
        val articleId: Int,
        val onBackClicked: () -> Unit,
        val onTokenClick: ((TokenMarketParams, AppCurrency) -> Unit),
        val preselectedArticlesId: List<Int> = emptyList(),
        val paginationConfig: NewsListConfig? = null,
    )
}