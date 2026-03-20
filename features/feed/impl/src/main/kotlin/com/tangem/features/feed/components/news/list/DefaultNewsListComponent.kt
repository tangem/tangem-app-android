package com.tangem.features.feed.components.news.list

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
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.LocalMainBottomSheetColor
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.news.model.NewsListConfig
import com.tangem.features.feed.model.news.list.NewsListModel
import com.tangem.features.feed.ui.news.list.NewsListContent
import kotlinx.serialization.Serializable

internal class DefaultNewsListComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableModularBottomSheetContentComponent, AppComponentContext by appComponentContext {

    private val newsListModel = getOrCreateModel<NewsListModel, Params>(params = params)

    @Composable
    override fun Title(bottomSheetState: State<BottomSheetState>) {
        val background = LocalMainBottomSheetColor.current.value
        val state by newsListModel.state.collectAsStateWithLifecycle()
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
            )
        } else {
            TangemTopAppBar(
                containerColor = background,
                title = stringResourceSafe(R.string.common_news),
                startButton = TopAppBarButtonUM.Icon(
                    iconRes = R.drawable.ic_back_24,
                    onClicked = state.onBackClick,
                    isEnabled = bottomSheetState.value == BottomSheetState.EXPANDED,
                ),
            )
        }
    }

    @Composable
    override fun Content(bottomSheetState: State<BottomSheetState>, modifier: Modifier) {
        val state by newsListModel.state.collectAsStateWithLifecycle()
        NewsListContent(
            state = state,
            modifier = modifier,
        )
    }

    @Serializable
    data class Params(
        val onArticleClicked: (
            currentArticle: Int,
            prefetchedArticles: List<Int>,
            paginationConfig: NewsListConfig?,
        ) -> Unit,
        val onBackClick: () -> Unit,
    )
}