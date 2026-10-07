package com.tangem.features.feed.crypto.categories.model.transformer

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.MarketCategory
import com.tangem.features.feed.crypto.categories.MarketCategorySelection
import com.tangem.features.feed.crypto.categories.model.converter.FeaturedCategoryConverter
import com.tangem.features.feed.crypto.categories.model.converter.MarketCategoryItemConverter
import com.tangem.features.feed.crypto.categories.ui.state.MarketCategoryListUM
import com.tangem.features.feed.crypto.categories.ui.state.MarketCategorySelectorUM
import com.tangem.features.feed.crypto.impl.R
import com.tangem.utils.transformer.Transformer

/**
 * Assembles the selector body: the featured badge rail above the full category list.
 *
 * A failed or empty featured request drops the rail rather than the screen — the full list is the
 * filter, the rail is only a shortcut into it.
 *
 * A search drops the rail too: the list is then a result set, and an unfiltered shortcut strip is not
 * one of the results.
 *
 * @property isSearching a query is applied to the listing on screen — not merely typed into the field
 */
@Suppress("LongParameterList")
internal class MarketCategorySelectorTransformer(
    private val featured: List<MarketCategory>,
    private val categories: List<MarketCategory>,
    private val selection: MarketCategorySelection,
    private val appCurrency: AppCurrency,
    private val isSearching: Boolean,
    private val isEdited: Boolean,
    private val isLoadingMore: Boolean,
    private val onLoadMore: () -> Unit,
    private val onSelectionChange: (MarketCategorySelection) -> Unit,
) : Transformer<MarketCategorySelectorUM> {

    override fun transform(prevState: MarketCategorySelectorUM): MarketCategorySelectorUM {
        return prevState.copy(
            isEdited = isEdited,
            content = MarketCategorySelectorUM.ContentUM.Content(
                featured = featured
                    .takeIf { !isSearching && it.isNotEmpty() }
                    ?.let { FeaturedCategoryConverter(selection, onSelectionChange).convert(it) },
                categories = MarketCategoryListUM(
                    title = resourceReference(R.string.market_categories),
                    items = MarketCategoryItemConverter(
                        selection = selection,
                        appCurrency = appCurrency,
                        onSelectionChange = onSelectionChange,
                    ).convert(categories),
                    isLoadingMore = isLoadingMore,
                    loadMore = onLoadMore,
                ),
            ),
        )
    }
}