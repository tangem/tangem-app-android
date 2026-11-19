package com.tangem.features.feed.rwa.categories.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import com.tangem.utils.annotations.RemoveWithToggle
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed interface CategorySelectorUM {

    data object Loading : CategorySelectorUM

    @Immutable
    data class Content(val items: ImmutableList<CategoryItemUM>) : CategorySelectorUM

    /** @property isApplyVisible a category + sector pair is picked that differs from the applied filter */
    @RemoveWithToggle("AND_19514_CATEGORY_FILTERS_REDESIGN_ENABLED")
    @Immutable
    data class LegacyContent(
        val sections: ImmutableList<LegacyCategorySelectorSectionUM>,
        val isApplyVisible: Boolean,
        val footer: LegacyCategorySelectorFooterUM,
    ) : CategorySelectorUM

    @Immutable
    data class Error(val onRetry: () -> Unit) : CategorySelectorUM
}

@RemoveWithToggle("AND_19514_CATEGORY_FILTERS_REDESIGN_ENABLED")
@Immutable
internal sealed interface LegacyCategorySelectorSectionUM {

    val title: TextReference

    @Immutable
    data class Sectors(
        override val title: TextReference,
        val sectors: ImmutableList<CategorySectorUM>,
    ) : LegacyCategorySelectorSectionUM

    @Immutable
    data class Items(
        override val title: TextReference,
        val items: ImmutableList<CategoryItemUM>,
    ) : LegacyCategorySelectorSectionUM
}

@RemoveWithToggle("AND_19514_CATEGORY_FILTERS_REDESIGN_ENABLED")
@Immutable
internal data class LegacyCategorySelectorFooterUM(val onApply: () -> Unit)

@Immutable
internal data class CategorySectorUM(
    val id: String,
    val label: TextReference,
    val isSelected: Boolean,
    val onItemClick: () -> Unit,
)

/**
 * @property caption trailing secondary line (e.g. "212 assets")
 * @property sectors chips shown under the row; empty unless the category has a sector choice
 */
@Immutable
internal data class CategoryItemUM(
    val id: String,
    val title: TextReference,
    val caption: TextReference,
    val isSelected: Boolean,
    val sectors: ImmutableList<CategorySectorUM>,
    val onItemClick: () -> Unit,
)