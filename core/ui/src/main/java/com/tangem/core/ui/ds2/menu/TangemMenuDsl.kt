package com.tangem.core.ui.ds2.menu

import androidx.compose.ui.graphics.vector.ImageVector
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.toPersistentList

/**
 * Builds a [TangemMenu.UiState].
 *
 * ```
 * tangemMenuUM {
 *     item(label = resourceReference(R.string.rename), iconStart = Icons.ic_edit_20) { rename() }
 *     divider(title = resourceReference(R.string.danger_zone))
 *     item(label = resourceReference(R.string.delete), variant = TangemMenu.ItemVariant.Error) { delete() }
 * }
 * ```
 *
 * @param variant Kind of anchor the menu opens from. See [TangemMenu.Variant].
 * @param isSelectable Figma `Has Selection`. See [TangemMenu.UiState.isSelectable].
 */
fun tangemMenuUM(
    variant: TangemMenu.Variant = TangemMenu.Variant.Button,
    isSelectable: Boolean = false,
    init: TangemMenuBuilder.() -> Unit,
): TangemMenu.UiState = TangemMenuBuilder().apply(init).build(variant = variant, isSelectable = isSelectable)

class TangemMenuBuilder internal constructor() {

    private val entries = mutableListOf<TangemMenu.UiState.Entry>()

    @Suppress("LongParameterList")
    fun item(
        label: TextReference,
        description: TextReference? = null,
        iconStart: ImageVector? = null,
        iconEnd: ImageVector? = null,
        variant: TangemMenu.ItemVariant = TangemMenu.ItemVariant.Default,
        isEnabled: Boolean = true,
        isSelected: Boolean = false,
        isDismissOnClick: Boolean = true,
        onClick: () -> Unit,
    ) {
        entries += TangemMenu.UiState.Item(
            label = label,
            onClick = onClick,
            description = description,
            iconStart = iconStart,
            iconEnd = iconEnd,
            variant = variant,
            isEnabled = isEnabled,
            isSelected = isSelected,
            isDismissOnClick = isDismissOnClick,
        )
    }

    fun divider(title: TextReference? = null) {
        entries += TangemMenu.UiState.Divider(title = title)
    }

    internal fun build(variant: TangemMenu.Variant, isSelectable: Boolean): TangemMenu.UiState =
        TangemMenu.UiState(entries = entries.toPersistentList(), variant = variant, isSelectable = isSelectable)
}