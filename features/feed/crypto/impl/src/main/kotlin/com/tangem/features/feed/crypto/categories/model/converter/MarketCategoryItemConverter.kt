package com.tangem.features.feed.crypto.categories.model.converter

import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.compact
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.MarketCategory
import com.tangem.features.feed.crypto.categories.MarketCategorySelection
import com.tangem.features.feed.crypto.categories.ui.state.MarketCategoryItemUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * Builds the rows of the full category list — one row per category, and nothing else.
 *
 * There is no "no filter" row: an unfiltered list simply checks nothing, and clearing an applied
 * category is the job of the clear button on the tab's filter chip.
 *
 * Re-tapping the selected row republishes the selection unchanged rather than clearing it — a row that
 * both selects and deselects reads as a broken radio group.
 */
internal class MarketCategoryItemConverter(
    private val selection: MarketCategorySelection,
    private val appCurrency: AppCurrency,
    private val onSelectionChange: (MarketCategorySelection) -> Unit,
) : Converter<List<MarketCategory>, ImmutableList<MarketCategoryItemUM>> {

    override fun convert(value: List<MarketCategory>): ImmutableList<MarketCategoryItemUM> {
        val categoryRows = value.map { category ->
            val isSelected = selection.category?.id == category.id

            MarketCategoryItemUM(
                id = category.id,
                title = stringReference(category.name),
                subtitle = stringReference(
                    category.marketCap.format {
                        fiat(
                            fiatCurrencyCode = appCurrency.code,
                            fiatCurrencySymbol = appCurrency.symbol,
                        ).compact()
                    },
                ),
                caption = pluralReference(
                    id = R.plurals.common_assets_count,
                    count = category.tokensCount,
                    formatArgs = wrappedList(category.tokensCount),
                ),
                isSelected = isSelected,
                onItemClick = {
                    onSelectionChange(
                        if (isSelected) {
                            selection
                        } else {
                            MarketCategorySelection(
                                category = MarketCategorySelection.Entry(
                                    id = category.id,
                                    name = category.name,
                                ),
                            )
                        },
                    )
                },
            )
        }

        return categoryRows.toImmutableList()
    }
}