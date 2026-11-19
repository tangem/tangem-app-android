package com.tangem.features.feed.rwa.categories.model.transformer

import com.tangem.domain.markets.CoinCategory
import com.tangem.features.feed.rwa.categories.CategorySelection
import com.tangem.features.feed.rwa.categories.model.converter.CategoryTagConverter
import com.tangem.features.feed.rwa.categories.ui.state.CategorySelectorUM
import com.tangem.utils.transformer.Transformer

internal class CategorySelectorTransformer(
    private val categoryList: List<CoinCategory>,
    private val categorySelection: CategorySelection,
    private val onSelectionChange: (CategorySelection) -> Unit,
) : Transformer<CategorySelectorUM> {

    override fun transform(prevState: CategorySelectorUM): CategorySelectorUM {
        return CategorySelectorUM.Content(
            items = CategoryTagConverter(
                categorySelection = categorySelection,
                onSelectionChange = onSelectionChange,
            ).convert(categoryList),
        )
    }
}