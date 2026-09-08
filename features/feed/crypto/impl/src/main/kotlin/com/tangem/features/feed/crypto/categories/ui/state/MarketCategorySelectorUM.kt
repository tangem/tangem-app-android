package com.tangem.features.feed.crypto.categories.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed interface MarketCategorySelectorUM {

    data object Loading : MarketCategorySelectorUM

    /**
     * @property featured null when the backend curates no featured categories or the featured request
     * failed — the rail is then hidden and the full list still works
     */
    @Immutable
    data class Content(
        val featured: FeaturedCategoriesUM?,
        val categories: MarketCategoryListUM,
        val isEdited: Boolean,
        val footer: MarketCategorySelectorFooterUM,
    ) : MarketCategorySelectorUM

    @Immutable
    data class Error(val onRetry: () -> Unit) : MarketCategorySelectorUM
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