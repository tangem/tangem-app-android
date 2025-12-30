package com.tangem.features.feed.components.news.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.R
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.decompose.ComposableModularBottomSheetContentComponent
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.LocalMainBottomSheetColor
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