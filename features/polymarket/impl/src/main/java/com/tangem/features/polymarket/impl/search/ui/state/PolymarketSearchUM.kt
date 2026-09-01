package com.tangem.features.polymarket.impl.search.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketEventUM
import kotlinx.collections.immutable.ImmutableList

/**
 * State of the events search screen.
 *
 * @property query raw text of the search field, echoed back on every change
 * @property onQueryChange called with the new field text on every keystroke
 * @property onCloseClick leaves the search
 * @property content what the area above the search field shows
 */
@Immutable
internal data class PolymarketSearchUM(
    val query: String,
    val onQueryChange: (String) -> Unit,
    val onCloseClick: () -> Unit,
    val content: ContentUM,
) {

    @Immutable
    sealed interface ContentUM {

        /** Nothing searched yet — the "start typing" prompt. */
        data object Initial : ContentUM

        /** The first page of a query is on its way. */
        data object Loading : ContentUM

        /**
         * @property events events matched so far, accumulated over the loaded pages
         * @property isLoadingNextPage whether the next page is on its way, shown as a footer loader
         */
        data class Results(
            val events: ImmutableList<PolymarketEventUM>,
            val isLoadingNextPage: Boolean,
        ) : ContentUM

        /** The query matched nothing. */
        data object NothingFound : ContentUM

        /**
         * The search could not be served.
         *
         * @property onReloadClick retries the current query
         */
        data class Error(val onReloadClick: () -> Unit) : ContentUM
    }
}