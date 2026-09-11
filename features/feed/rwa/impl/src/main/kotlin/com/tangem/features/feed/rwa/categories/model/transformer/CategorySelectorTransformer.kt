package com.tangem.features.feed.rwa.categories.model.transformer

import com.tangem.domain.markets.CoinCategory
import com.tangem.features.feed.rwa.categories.CategorySelection
import com.tangem.features.feed.rwa.categories.model.converter.CategorySectorConverter
import com.tangem.features.feed.rwa.categories.model.converter.CategoryTagConverter
import com.tangem.features.feed.rwa.categories.ui.state.CategorySelectorFooterUM
import com.tangem.features.feed.rwa.categories.ui.state.CategorySelectorUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toPersistentList

internal class CategorySelectorTransformer(
    private val categoryList: List<CoinCategory>,
    private val categorySelection: CategorySelection,
    private val isEdited: Boolean,
    private val onReset: () -> Unit,
    private val onApply: () -> Unit,
    private val onSelectionChange: (CategorySelection) -> Unit,
) : Transformer<CategorySelectorUM> {

    override fun transform(prevState: CategorySelectorUM): CategorySelectorUM {
        val selectedCategory = categoryList.firstOrNull { it.id == categorySelection.category?.id }

        return CategorySelectorUM.Content(
            sections = listOfNotNull(
                CategoryTagConverter(
                    categorySelection = categorySelection,
                    onSelectionChange = onSelectionChange,
                ).convert(categoryList),
                // a lone sector filters no differently than its category, so the rail earns its place
                // only from two sectors up — below that the category row is the whole filter
                selectedCategory
                    ?.takeIf { it.sectors.size > 1 }
                    ?.let { category ->
                        CategorySectorConverter(
                            categorySelection = categorySelection,
                            onSelectionChange = onSelectionChange,
                        ).convert(category)
                    },
            ).toPersistentList(),
            isEdited = isEdited,
            footer = CategorySelectorFooterUM(onReset = onReset, onApply = onApply),
        )
    }
}