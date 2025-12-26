package com.tangem.features.feed.components.news.details

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
import com.tangem.core.ui.res.LocalMainBottomSheetColor
import com.tangem.features.feed.model.news.details.NewsDetailsModel
import com.tangem.features.feed.ui.news.details.NewsDetailsContent
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
                isEnabled = bottomSheetState.value == BottomSheetState.EXPANDED,
            ),
        )
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
        val articleId: Int,
        val onBackClicked: () -> Unit,
        val preselectedArticlesId: List<Int> = emptyList(),
        val tokenIds: List<String> = emptyList(),
        val categoryIds: List<Int> = emptyList(),
    )
}