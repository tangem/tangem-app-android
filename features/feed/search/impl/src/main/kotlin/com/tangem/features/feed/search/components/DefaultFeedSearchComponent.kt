package com.tangem.features.feed.search.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.features.feed.nav.FeedRoute
import com.tangem.features.feed.search.FeedSearchComponent
import com.tangem.features.feed.search.model.FeedSearchModel
import com.tangem.features.feed.search.ui.FeedSearchContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Stable
internal class DefaultFeedSearchComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: FeedRoute.Search,
) : FeedSearchComponent, AppComponentContext by context {

    private val model: FeedSearchModel = getOrCreateModel(params)

    @Composable
    override fun Content(
        bottomSheetState: State<BottomSheetState>,
        contentPadding: PaddingValues,
        onExpandSheet: () -> Unit,
        modifier: Modifier,
    ) {
        val state by model.uiState.collectAsStateWithLifecycle()

        FeedSearchContent(
            state = state,
            contentPadding = contentPadding,
            modifier = modifier,
        )
    }

    @AssistedFactory
    interface Factory : FeedSearchComponent.Factory {
        override fun create(context: AppComponentContext, params: FeedRoute.Search): DefaultFeedSearchComponent
    }
}