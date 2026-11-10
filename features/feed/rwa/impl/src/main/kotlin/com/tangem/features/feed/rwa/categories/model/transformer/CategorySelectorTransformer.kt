package com.tangem.features.feed.rwa.categories.model.transformer

import com.tangem.domain.markets.CoinCategory
import com.tangem.features.feed.rwa.categories.CategorySelection
import com.tangem.features.feed.rwa.categories.model.converter.CategorySectorConverter
import com.tangem.features.feed.rwa.categories.model.hasSectorChoice
import com.tangem.features.feed.rwa.categories.model.converter.CategoryTagConverter
import com.tangem.features.feed.rwa.categories.ui.state.CategorySelectorFooterUM
import com.tangem.features.feed.rwa.categories.ui.state.CategorySelectorUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toPersistentList

internal class CategorySelectorTransformer(
    private val categoryList: List<CoinCategory>,
    private val categorySelection: CategorySelection,
    private val isApplyVisible: Boolean,
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
                selectedCategory
                    ?.takeIf { it.hasSectorChoice }
                    ?.let { category ->
                        CategorySectorConverter(
                            categorySelection = categorySelection,
                            onSelectionChange = onSelectionChange,
                        ).convert(category)
                    },
            ).toPersistentList(),
            isApplyVisible = isApplyVisible,
            footer = CategorySelectorFooterUM(onApply = onApply),
        )
    }
}