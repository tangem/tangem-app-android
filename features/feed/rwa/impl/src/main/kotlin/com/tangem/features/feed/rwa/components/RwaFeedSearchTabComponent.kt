package com.tangem.features.feed.rwa.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.features.feed.nav.FeedTabComponent
import com.tangem.features.feed.rwa.model.search.RwaFeedSearchTabModel
import com.tangem.features.feed.rwa.ui.RwaSearchContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow

/**
 * Real assets tab of the feed's search screen.
 *
 * A separate component from [RwaFeedTabComponent] rather than a mode of it: the two share only the asset
 * row, and a single component would build the tab's Featured rail and filters on the search screen.
 */
@Stable
internal class RwaFeedSearchTabComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: Params,
) : FeedTabComponent, AppComponentContext by context {

    private val model: RwaFeedSearchTabModel = getOrCreateModel(params)

    // owned by the component: the page stays CREATED while unselected, so scroll survives switches
    private val listState = LazyListState()

    @Composable
    override fun Content(bottomSheetState: State<BottomSheetState>, contentPadding: PaddingValues, modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        val sheetState = bottomSheetState.value
        LaunchedEffect(sheetState) {
            model.containerBottomSheetState.value = sheetState
        }

        LifecycleStartEffect(Unit) {
            model.isVisibleOnScreen.value = true
            onStopOrDispose {
                model.isVisibleOnScreen.value = false
            }
        }

        RwaSearchContent(
            state = state,
            listState = listState,
            contentPadding = contentPadding,
            modifier = modifier,
        )
    }

    /**
     * @property query the search bar's text, raw. The tab owns debouncing and trimming, and must not
     * fetch while it is blank — see `FeedTabContributor.createSearchTab`.
     */
    data class Params(val query: StateFlow<String>)

    @AssistedFactory
    interface Factory {
        fun create(context: AppComponentContext, params: Params): RwaFeedSearchTabComponent
    }
}