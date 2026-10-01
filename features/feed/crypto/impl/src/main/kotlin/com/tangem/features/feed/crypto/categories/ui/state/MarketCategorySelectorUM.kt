package com.tangem.features.feed.crypto.categories.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds2.search.TangemSearch
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class MarketCategorySelectorUM(
    val search: TangemSearch.State,
    val isEdited: Boolean,
    val footer: MarketCategorySelectorFooterUM,
    val content: ContentUM,
) {

    @Immutable
    sealed interface ContentUM {

        data object Loading : ContentUM

        /**
         * @property featured null when the backend curates no featured categories, the featured
         * request failed, or a search is applied — the rail is then hidden and the list still works
         */
        @Immutable
        data class Content(
            val featured: FeaturedCategoriesUM?,
            val categories: MarketCategoryListUM,
        ) : ContentUM

        /** A search matched nothing. An unfiltered listing keeps the list body even when it is empty. */
        data object Empty : ContentUM

        @Immutable
        data class Error(val onRetry: () -> Unit) : ContentUM
    }
}

@Immutable
internal data class FeaturedCategoriesUM(
    val title: TextReference,
    val items: ImmutableList<FeaturedCategoryUM>,
)

@Immutable
internal data class FeaturedCategoryUM(
    val id: String,
    val label: TextReference,
    val isSelected: Boolean,
    val onItemClick: () -> Unit,
)

/** @property isLoadingMore next page in flight — the list shows a trailing shimmer row */
@Immutable
internal data class MarketCategoryListUM(
    val title: TextReference,
    val items: ImmutableList<MarketCategoryItemUM>,
    val isLoadingMore: Boolean,
    val loadMore: () -> Unit,
)

/** @property caption trailing secondary line (e.g. "212 coins"); null on the synthetic "All" row */
@Immutable
internal data class MarketCategoryItemUM(
    val id: String,
    val title: TextReference,
    val subtitle: TextReference?,
    val caption: TextReference?,
    val isSelected: Boolean,
    val onItemClick: () -> Unit,
)

@Immutable
internal data class MarketCategorySelectorFooterUM(
    val onReset: () -> Unit,
    val onApply: () -> Unit,
)