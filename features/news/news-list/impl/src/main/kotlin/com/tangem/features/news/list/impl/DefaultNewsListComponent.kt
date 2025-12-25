package com.tangem.features.news.list.impl

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.features.news.list.api.NewsListComponent
import com.tangem.features.news.list.impl.ui.NewsListContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultNewsListComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: NewsListComponent.Params,
) : NewsListComponent, AppComponentContext by context {

    private val model: NewsListModel = getOrCreateModel(params)

    @Composable
    override fun BottomSheetContent(
        bottomSheetState: State<BottomSheetState>,
        onHeaderSizeChange: (Dp) -> Unit,
        modifier: Modifier,
    ) {
        val uiState by model.uiState.collectAsStateWithLifecycle()
        val bsState by bottomSheetState

        BackHandler(enabled = bsState == BottomSheetState.EXPANDED) {
            navigateBack()
        }

        NewsListContent(
            state = uiState,
            onBackClick = ::navigateBack,
            modifier = modifier,
        )
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val uiState by model.uiState.collectAsStateWithLifecycle()
        NewsListContent(
            state = uiState,
            onBackClick = model::onBackClick,
            modifier = modifier,
        )
    }

    private fun navigateBack() = router.pop()

    @AssistedFactory
    interface Factory : NewsListComponent.Factory {
        override fun create(context: AppComponentContext, params: NewsListComponent.Params): DefaultNewsListComponent
    }
}