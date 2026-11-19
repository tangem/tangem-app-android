package com.tangem.features.feed.rwa.categories.model.transformer

import com.tangem.domain.markets.CoinCategory
import com.tangem.features.feed.rwa.categories.CategorySelection
import com.tangem.features.feed.rwa.categories.model.converter.LegacyCategorySectorConverter
import com.tangem.features.feed.rwa.categories.model.hasSectorChoice
import com.tangem.features.feed.rwa.categories.model.converter.LegacyCategoryTagConverter
import com.tangem.features.feed.rwa.categories.ui.state.LegacyCategorySelectorFooterUM
import com.tangem.features.feed.rwa.categories.ui.state.CategorySelectorUM
import com.tangem.utils.annotations.RemoveWithToggle
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toPersistentList

@RemoveWithToggle("AND_19514_CATEGORY_FILTERS_REDESIGN_ENABLED")
internal class LegacyCategorySelectorTransformer(
    private val categoryList: List<CoinCategory>,
    private val categorySelection: CategorySelection,
    private val isApplyVisible: Boolean,
    private val onApply: () -> Unit,
    private val onSelectionChange: (CategorySelection) -> Unit,
) : Transformer<CategorySelectorUM> {

    override fun transform(prevState: CategorySelectorUM): CategorySelectorUM {
        val selectedCategory = categoryList.firstOrNull { it.id == categorySelection.category?.id }

        return CategorySelectorUM.LegacyContent(
            sections = listOfNotNull(
                LegacyCategoryTagConverter(
                    categorySelection = categorySelection,
                    onSelectionChange = onSelectionChange,
                ).convert(categoryList),
                selectedCategory
                    ?.takeIf { it.hasSectorChoice }
                    ?.let { category ->
                        LegacyCategorySectorConverter(
                            categorySelection = categorySelection,
                            onSelectionChange = onSelectionChange,
                        ).convert(category)
                    },
            ).toPersistentList(),
            isApplyVisible = isApplyVisible,
            footer = LegacyCategorySelectorFooterUM(onApply = onApply),
        )
    }
}