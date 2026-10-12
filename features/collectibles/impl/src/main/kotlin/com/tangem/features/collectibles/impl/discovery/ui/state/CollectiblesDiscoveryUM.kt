package com.tangem.features.collectibles.impl.discovery.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds2.search.TangemSearch
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

/**
 * @property search hoisted state of the search field; [TangemSearch.State.isActive] also drives whether
 *   the host lays out its leading blocks at all.
 * @property hasSearchableCatalogue the categories report a non-empty catalogue. Read from the categories
 *   rather than from the packs on screen, so the search field survives a tab with no packs and every trip
 *   through [ContentUM.Loading].
 */
internal data class CollectiblesDiscoveryUM(
    val tabs: TabsUM,
    val content: ContentUM,
    val search: TangemSearch.State,
    val hasSearchableCatalogue: Boolean = false,
) {

    @Immutable
    sealed interface TabsUM {

        data object Loading : TabsUM

        /** The categories could not be read — the band is not laid out and the catalogue stays unfiltered. */
        data object Hidden : TabsUM

        data class Content(val tabs: ImmutableList<TangemTabItemUM>) : TabsUM
    }

    @Immutable
    sealed interface ContentUM {

        data object Loading : ContentUM

        /**
         * @property rows packs laid out two per row; a row is one item of the host's lazy list
         * @property isLoadingNextPage whether the footer loader of the next page is shown
         * @property isReloading the catalogue is being refetched
         */
        data class Content(
            val rows: ImmutableList<PackRowUM>,
            val isLoadingNextPage: Boolean,
            val isReloading: Boolean,
        ) : ContentUM

        data class Error(val message: TextReference, val onReloadClick: () -> Unit) : ContentUM

        data class Empty(val message: TextReference) : ContentUM
    }

    /** @property id id of the first pack in the row — the row's stable identity as a list item */
    data class PackRowUM(
        val id: String,
        val packs: ImmutableList<PackUM>,
    )

    data class PackUM(
        val id: String,
        val imageUrl: String,
        val title: TextReference,
        val price: TextReference,
        val badge: TextReference?,
        val onClick: () -> Unit,
    )
}