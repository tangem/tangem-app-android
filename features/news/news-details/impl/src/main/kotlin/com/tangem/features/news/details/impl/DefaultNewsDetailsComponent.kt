package com.tangem.features.news.details.impl

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
import com.tangem.features.news.details.api.NewsDetailsComponent
import com.tangem.features.news.details.impl.ui.NewsDetailsContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultNewsDetailsComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: NewsDetailsComponent.Params,
) : NewsDetailsComponent, AppComponentContext by context {

    private val model: NewsDetailsModel = getOrCreateModel(params)

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

        NewsDetailsContent(
            state = uiState,
            onBackClick = ::navigateBack,
            modifier = modifier,
            isBottomSheetMode = true,
        )
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val uiState by model.uiState.collectAsStateWithLifecycle()
        NewsDetailsContent(
            state = uiState,
            onBackClick = model::onBackClick,
            modifier = modifier,
            isBottomSheetMode = false,
        )
    }

    private fun navigateBack() = router.pop()

    @AssistedFactory
    interface Factory : NewsDetailsComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: NewsDetailsComponent.Params,
        ): DefaultNewsDetailsComponent
    }
}