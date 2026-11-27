package com.tangem.features.collectibles.impl.inventory.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds2.search.TangemSearch
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

/**
 * @property contentKey identity of the list on screen: changes when it is replaced by another one (another tab,
 *   search opened). A new key shows the new list at once, without item animations, scrolled to the top; changes
 *   under the same key (refresh, a card added or removed) animate.
 */
internal data class CollectiblesInventoryUM(
    val tabs: ImmutableList<TangemTabItemUM>,
    val contentKey: String,
    val content: ContentUM,
    val isRefreshing: Boolean,
    val search: TangemSearch.State,
    val isSearchMode: Boolean,
) {

    @Immutable
    sealed interface ContentUM {

        data class Loading(val info: ItemUM.Info?) : ContentUM

        data class Items(val items: ImmutableList<ItemUM>) : ContentUM

        data class Empty(val message: TextReference) : ContentUM

        data class Error(
            val message: TextReference,
            val isReloading: Boolean,
            val onReloadClick: () -> Unit,
        ) : ContentUM
    }

    @Immutable
    sealed interface ItemUM {

        data class Info(val text: TextReference) : ItemUM

        /**
         * @property timeLeft what is left of the buyback window; `null` on the collection tab
         */
        data class Card(
            val id: String,
            val imageUrl: String,
            val title: TextReference,
            val price: TextReference,
            val grade: TextReference,
            val timeLeft: TextReference?,
            val onClick: () -> Unit,
        ) : ItemUM
    }
}