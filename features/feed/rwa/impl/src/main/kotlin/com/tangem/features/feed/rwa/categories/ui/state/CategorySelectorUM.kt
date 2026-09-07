package com.tangem.features.feed.rwa.categories.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed interface CategorySelectorUM {

    data object Loading : CategorySelectorUM

    @Immutable
    data class Content(
        val sections: ImmutableList<CategorySelectorSectionUM>,
        val isEdited: Boolean,
        val footer: CategorySelectorFooterUM,
    ) : CategorySelectorUM

    @Immutable
    data class Error(val onRetry: () -> Unit) : CategorySelectorUM
}

@Immutable
internal sealed interface CategorySelectorSectionUM {

    val title: TextReference

    @Immutable
    data class Sectors(
        override val title: TextReference,
        val sectors: ImmutableList<CategorySectorUM>,
    ) : CategorySelectorSectionUM

    @Immutable
    data class Items(
        override val title: TextReference,
        val items: ImmutableList<CategoryItemUM>,
    ) : CategorySelectorSectionUM
}

@Immutable
internal data class CategorySectorUM(
    val id: String,
    val label: TextReference,
    val isSelected: Boolean,
    val onItemClick: () -> Unit,
)

/**
 * @property caption trailing secondary line (e.g. "212 assets")
 */
@Immutable
internal data class CategoryItemUM(
    val id: String,
    val title: TextReference,
    val caption: TextReference,
    val isSelected: Boolean,
    val onItemClick: () -> Unit,
)

@Immutable
internal data class CategorySelectorFooterUM(
    val onReset: () -> Unit,
    val onApply: () -> Unit,
)