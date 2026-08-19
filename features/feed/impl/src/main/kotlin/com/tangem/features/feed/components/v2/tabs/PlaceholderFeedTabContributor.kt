package com.tangem.features.feed.components.v2.tabs

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.feed.nav.FeedTabComponent
import com.tangem.features.feed.nav.FeedTabContributor
import com.tangem.features.feed.nav.FeedTabId
import com.tangem.features.feed.ui.v2.FeedTabPlaceholderList

/** Stub contributor for tabs whose real module is not plugged in yet. TODO: [TWI-1608] */
internal class PlaceholderFeedTabContributor(
    override val id: FeedTabId,
    override val title: TextReference,
) : FeedTabContributor {

    override val isAvailable: Boolean = true

    override fun createTab(context: AppComponentContext): FeedTabComponent {
        return PlaceholderFeedTabComponent(context = context, tabId = id)
    }
}

private class PlaceholderFeedTabComponent(
    context: AppComponentContext,
    private val tabId: FeedTabId,
) : FeedTabComponent, AppComponentContext by context {

    // owned by the component: the page stays CREATED while unselected, so scroll survives switches
    private val listState = LazyListState()

    @Composable
    override fun Content(bottomSheetState: State<BottomSheetState>, contentPadding: PaddingValues, modifier: Modifier) {
        FeedTabPlaceholderList(
            tabId = tabId.value,
            listState = listState,
            contentPadding = contentPadding,
            onItemClick = null,
            modifier = modifier,
        )
    }
}