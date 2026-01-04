package com.tangem.features.feed.components.feed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.decompose.ComposableModularBottomSheetContentComponent
import com.tangem.features.feed.model.feed.FeedComponentModel
import com.tangem.features.feed.model.feed.FeedModelClickIntents
import com.tangem.features.feed.ui.feed.FeedList
import com.tangem.features.feed.ui.feed.FeedListHeader

internal class DefaultFeedComponent(
    appComponentContext: AppComponentContext,
    private val params: FeedParams,
) : ComposableModularBottomSheetContentComponent, AppComponentContext by appComponentContext {

    private val feedComponentModel = getOrCreateModel<FeedComponentModel, FeedParams>(params = params)

    @Composable
    override fun Title(bottomSheetState: State<BottomSheetState>) {
        val state by feedComponentModel.state.collectAsStateWithLifecycle()
        FeedListHeader(state.feedListSearchBar)
    }

    @Composable
    override fun Content(bottomSheetState: State<BottomSheetState>, modifier: Modifier) {
        LifecycleStartEffect(Unit) {
            feedComponentModel.isVisibleOnScreen.value = true
            onStopOrDispose {
                feedComponentModel.isVisibleOnScreen.value = false
            }
        }

        val state by feedComponentModel.state.collectAsStateWithLifecycle()
        FeedList(
            modifier = modifier,
            state = state,
        )
    }

    data class FeedParams(val feedClickIntents: FeedModelClickIntents)
}