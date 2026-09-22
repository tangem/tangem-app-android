package com.tangem.features.feed.crypto.categories.model.converter

import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resourceReference
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
 * Builds the rows of the full category list, prepending the synthetic "All" row unless a search narrows it.
 *
 * "All" stands for *no* category filter, so it emits the empty [MarketCategorySelection] and checks
 * itself when nothing is applied — the in-screen counterpart of the tab chip's clear button. Its id
 * never travels in a selection.
 *
 * Re-tapping the selected row republishes the selection unchanged rather than clearing it: clearing
 * has a dedicated row, and a row that both selects and deselects reads as a broken radio group.
 *
 * @property shouldIncludeAllRow false while a search is applied — the list is then a result set, and
 * "no filter" is not one of the results
 */
internal class MarketCategoryItemConverter(
    private val selection: MarketCategorySelection,
    private val appCurrency: AppCurrency,
    private val shouldIncludeAllRow: Boolean,
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

        return if (shouldIncludeAllRow) {
            (listOf(createAllRow()) + categoryRows).toImmutableList()
        } else {
            categoryRows.toImmutableList()
        }
    }

    private fun createAllRow() = MarketCategoryItemUM(
        id = ALL_CATEGORIES_ID,
        title = resourceReference(R.string.common_all),
        subtitle = null,
        caption = null,
        isSelected = selection.category == null,
        onItemClick = { onSelectionChange(MarketCategorySelection()) },
    )

    private companion object {
        const val ALL_CATEGORIES_ID = "__all__"
    }
}