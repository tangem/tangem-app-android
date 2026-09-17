package com.tangem.features.collectibles.impl.discovery

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Stable
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableListContentComponent
import com.tangem.features.collectibles.impl.discovery.ui.state.CollectiblesDiscoveryUM

/**
 * The packs catalogue — category tabs and the paginated pack grid — contributed as items into a host's
 * lazy list.
 */
@Stable
internal interface CollectiblesDiscoveryComponent : ComposableListContentComponent<CollectiblesDiscoveryUM> {

    /** Shared by the inline tab band and the host's pinned overlay, so its horizontal scroll survives docking. */
    val tabScrollState: ScrollState

    /** Requests the next page; the model ignores it while a page is on its way or the list has ended. */
    fun onLoadMore()

    /** Refetches the catalogue from page one, keeping the selected category and the query. */
    fun onRefresh()

    interface Factory {
        fun create(context: AppComponentContext): CollectiblesDiscoveryComponent
    }
}