package com.tangem.features.polymarket.impl.common.feed.model.converter

import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.polymarket.model.PolymarketCategory
import com.tangem.features.polymarket.impl.common.feed.ui.state.PolymarketCategoryTabUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * Converts the loaded categories into the tab band of the Discovery feed.
 *
 * @property onCategoryClick called with the id of the tapped category
 */
internal class PolymarketCategoryTabUMConverter(
    private val onCategoryClick: (Int) -> Unit,
) : Converter<PolymarketCategoryTabUMConverter.Input, ImmutableList<PolymarketCategoryTabUM>> {

    override fun convert(value: Input): ImmutableList<PolymarketCategoryTabUM> = value.categories
        .map { category ->
            PolymarketCategoryTabUM(
                id = category.id,
                label = stringReference(category.label),
                icon = category.icon.takeIf { value.shouldShowIcons },
                isSelected = category.id == value.selectedCategoryId,
                onClick = { onCategoryClick(category.id) },
            )
        }
        .toImmutableList()

    fun subcategories(parent: PolymarketCategory, selectedId: Int?): ImmutableList<PolymarketCategoryTabUM> {
        if (parent.subcategories.isEmpty()) return emptyList<PolymarketCategoryTabUM>().toImmutableList()
        val all = PolymarketCategoryTabUM(
            id = parent.id,
            label = resourceReference(R.string.common_all),
            icon = null,
            isSelected = selectedId == null,
            onClick = { onCategoryClick(parent.id) },
        )
        val children = convert(Input(parent.subcategories, selectedId, shouldShowIcons = false))
        return (listOf(all) + children).toImmutableList()
    }

    /**
     * @property categories categories in the order the backend serves them
     * @property selectedCategoryId id of the category the feed is filtered by, `null` while there is none to select
     */
    data class Input(
        val categories: List<PolymarketCategory>,
        val selectedCategoryId: Int?,
        val shouldShowIcons: Boolean = true,
    )
}