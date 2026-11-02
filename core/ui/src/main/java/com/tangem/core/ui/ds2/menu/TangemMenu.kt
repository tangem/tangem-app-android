@file:Suppress("MagicNumber")

package com.tangem.core.ui.ds2.menu

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.tangem.core.ui.ds2.DsVersion
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_chevron_right_20
import com.tangem.core.ui.res.generated.icons.ic_copy_20
import com.tangem.core.ui.res.generated.icons.ic_edit_20
import com.tangem.core.ui.res.generated.icons.ic_eye_20
import com.tangem.core.ui.res.generated.icons.ic_share_android_20
import com.tangem.core.ui.res.generated.icons.ic_trash_bin_20
import kotlinx.collections.immutable.ImmutableList

/**
 * Design-system v2 menu: a popup list of actions anchored to the composable it is placed next to. Fill it with
 * [TangemMenu.Item]s split into groups by [TangemMenu.Divider]s.
 *
 * Version: 1.0.0
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=12011-4433)
 *
 * Behavior notes:
 * - Place it in the same parent as the anchor (e.g. a `Box` around the button, or around the value of a row): the
 *   menu opens below the anchor, end-aligned with it, at the [TangemMenu.Variant] gap. It flips above the anchor
 *   when there is no room below, and to start alignment when there is no room towards the start.
 * - The menu is at least 240dp wide and as wide as its widest item otherwise; items fill that width.
 * - Content taller than the window scrolls.
 * - The menu takes no taps while it closes.
 *
 * @param isExpanded Whether the menu is shown.
 * @param onDismissRequest Called on a tap outside the menu or on back press.
 * @param modifier Modifier for the menu container.
 * @param variant Kind of anchor the menu opens from. See [TangemMenu.Variant].
 * @param isSelectable Figma `Has Selection`: every item reserves a leading checkmark column, shown on items with
 *   `isSelected`, and divider titles shift to stay aligned with the item labels.
 * @param offset Extra offset on top of the variant gap, mirrored with the menu when it flips. Ignored when
 *   [positionProvider] is set.
 * @param positionProvider Custom popup placement; `null` anchors the menu as described above.
 * @param properties Popup window properties. Keep `clippingEnabled = false`: the window extends past the menu to
 *   fit its shadow, and a clipped window would be pushed back on screen together with the menu.
 * @param content Menu entries, typically [TangemMenu.Item]s and [TangemMenu.Divider]s.
 */
@Suppress("LongParameterList")
@Composable
fun TangemMenu(
    isExpanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    variant: TangemMenu.Variant = TangemMenu.Variant.Button,
    isSelectable: Boolean = false,
    offset: DpOffset = DpOffset.Zero,
    positionProvider: PopupPositionProvider? = null,
    properties: PopupProperties = PopupProperties(focusable = true, clippingEnabled = false),
    content: @Composable ColumnScope.() -> Unit,
) {
    TangemMenuPopup(
        isExpanded = isExpanded,
        onDismissRequest = onDismissRequest,
        variant = variant,
        offset = offset,
        positionProvider = positionProvider,
        properties = properties,
    ) { listModifier ->
        TangemMenuPanel(
            modifier = modifier,
            listModifier = listModifier,
            variant = variant,
            isSelectable = isSelectable,
            content = content,
        )
    }
}

/**
 * [TangemMenu] rendered from a [TangemMenu.UiState] — for menu content kept in UI state or built with the
 * [tangemMenuUM] DSL.
 *
 * Tapping an item whose [TangemMenu.UiState.Item.isDismissOnClick] is `true` calls [onDismissRequest] before the
 * item's click action.
 *
 * @param state Menu content. See [TangemMenu.UiState].
 * @param isExpanded Whether the menu is shown.
 * @param onDismissRequest Called on a tap outside the menu, on back press, or after a dismissing item is tapped.
 * @param modifier Modifier for the menu container.
 * @param offset Extra offset on top of the variant gap. Ignored when [positionProvider] is set.
 * @param positionProvider Custom popup placement; `null` anchors the menu to the composable it is placed next to.
 * @param properties Popup window properties.
 */
@Suppress("LongParameterList")
@Composable
fun TangemMenu(
    state: TangemMenu.UiState,
    isExpanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset.Zero,
    positionProvider: PopupPositionProvider? = null,
    properties: PopupProperties = PopupProperties(focusable = true, clippingEnabled = false),
) {
    TangemMenu(
        isExpanded = isExpanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        variant = state.variant,
        isSelectable = state.isSelectable,
        offset = offset,
        positionProvider = positionProvider,
        properties = properties,
    ) {
        TangemMenuEntries(entries = state.entries, onDismissRequest = onDismissRequest)
    }
}

/** Public API surface of [TangemMenu]. */
object TangemMenu {

    val version = DsVersion(major = 1, minor = 0, patch = 0)

    /**
     * Kind of anchor the menu opens from. The anchor plays its own press feedback; the variant places the menu and
     * tunes its motion to match it.
     */
    enum class Variant {
        /**
         * Opened from a button, e.g. the "•••" of a top bar: 8dp below the button. Item highlight eases in and out
         * symmetrically.
         */
        Button,

        /**
         * Opened from a row, e.g. a value picker in a settings list. Anchor the menu to the row's value, not the
         * whole row: it opens 12dp below the value and overlaps the rest of the row. Item highlight eases out, then
         * in.
         */
        Row,
    }

    /** Color scheme of a [TangemMenu.Item]. */
    enum class ItemVariant {
        Default,

        /** Start icon, label and description in the error color — for destructive actions. */
        Error,
    }

    /**
     * Content of [TangemMenu]. Build it with [tangemMenuUM].
     *
     * @param entries Items and dividers, top to bottom.
     * @param variant Kind of anchor the menu opens from. See [Variant].
     * @param isSelectable Figma `Has Selection`. See the `isSelectable` parameter of [TangemMenu].
     */
    @Immutable
    data class UiState(
        val entries: ImmutableList<Entry>,
        val variant: Variant = Variant.Button,
        val isSelectable: Boolean = false,
    ) {

        sealed interface Entry

        /**
         * One row of the menu. See [TangemMenu.Item] for the meaning of the fields.
         *
         * @param isDismissOnClick Closes the menu when the item is tapped.
         */
        data class Item(
            val label: TextReference,
            val onClick: () -> Unit,
            val description: TextReference? = null,
            val iconStart: ImageVector? = null,
            val iconEnd: ImageVector? = null,
            val variant: ItemVariant = ItemVariant.Default,
            val isEnabled: Boolean = true,
            val isSelected: Boolean = false,
            val isDismissOnClick: Boolean = true,
        ) : Entry

        /**
         * Separator between groups of items.
         *
         * @param title Group caption under the line; `null` draws the line alone.
         */
        data class Divider(val title: TextReference? = null) : Entry
    }

    /**
     * Row of [TangemMenu]: an optional checkmark column, an optional start icon, a single-line label with an
     * optional description, and an optional end icon. The whole row is clickable and is highlighted while pressed.
     *
     * Behavior notes:
     * - The checkmark column is laid out only when the hosting [TangemMenu] is `isSelectable`.
     * - [ItemVariant.Error] tints the start icon, label and description; the end icon keeps its color.
     * - A disabled row is muted in every part and ignores taps.
     *
     * Accessibility: announced as a button labelled with [label]; in a selectable menu it also reports its
     * selected state.
     *
     * @param label Row label, cut with an ellipsis after one line.
     * @param onClick Click handler of the row.
     * @param modifier Modifier for the row.
     * @param description Line under the label, cut after one line; `null` hides it.
     * @param iconStart 20dp icon before the label; `null` hides it.
     * @param iconEnd 20dp icon at the end of the row; `null` hides it.
     * @param variant Color scheme. See [ItemVariant].
     * @param isEnabled `false` mutes the row and ignores taps.
     * @param isSelected Shows the checkmark. Ignored unless the hosting menu is `isSelectable`.

     */
    @Suppress("LongParameterList")
    @Composable
    fun Item(
        label: TextReference,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        description: TextReference? = null,
        iconStart: ImageVector? = null,
        iconEnd: ImageVector? = null,
        variant: ItemVariant = ItemVariant.Default,
        isEnabled: Boolean = true,
        isSelected: Boolean = false,
        interactionSource: MutableInteractionSource? = null,
    ) {
        TangemMenuItemLayout(
            label = label,
            onClick = onClick,
            modifier = modifier,
            description = description,
            iconStart = iconStart,
            iconEnd = iconEnd,
            variant = variant,
            isEnabled = isEnabled,
            isSelected = isSelected,
            interactionSource = interactionSource,
        )
    }

    /**
     * Separator of [TangemMenu]: a hairline with an optional group [title] under it. In a selectable menu the
     * title is indented to line up with the item labels.
     *
     * @param modifier Modifier for the divider.
     * @param title Group caption; `null` draws the line alone.
     */
    @Composable
    fun Divider(modifier: Modifier = Modifier, title: TextReference? = null) {
        TangemMenuDividerLayout(title = title, modifier = modifier)
    }
}

@Composable
private fun TangemMenuEntries(entries: ImmutableList<TangemMenu.UiState.Entry>, onDismissRequest: () -> Unit) {
    entries.fastForEach { entry ->
        when (entry) {
            is TangemMenu.UiState.Item -> TangemMenu.Item(
                label = entry.label,
                onClick = {
                    if (entry.isDismissOnClick) onDismissRequest()
                    entry.onClick()
                },
                description = entry.description,
                iconStart = entry.iconStart,
                iconEnd = entry.iconEnd,
                variant = entry.variant,
                isEnabled = entry.isEnabled,
                isSelected = entry.isSelected,
            )
            is TangemMenu.UiState.Divider -> TangemMenu.Divider(title = entry.title)
        }
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemMenuPreview() {
    TangemThemePreviewRedesign {
        PreviewMenus(
            states = listOf(
                tangemMenuUM {
                    item(label = stringReference("Copy address"), iconStart = Icons.ic_copy_20) {}
                    item(label = stringReference("Share"), iconStart = Icons.ic_share_android_20) {}
                    item(
                        label = stringReference("Rename"),
                        description = stringReference("Visible only to you"),
                        iconStart = Icons.ic_edit_20,
                        iconEnd = Icons.ic_chevron_right_20,
                    ) {}
                    divider(title = stringReference("Danger zone"))
                    item(label = stringReference("Hide"), iconStart = Icons.ic_eye_20, isEnabled = false) {}
                    item(
                        label = stringReference("Delete"),
                        description = stringReference("This can't be undone"),
                        iconStart = Icons.ic_trash_bin_20,
                        variant = TangemMenu.ItemVariant.Error,
                    ) {}
                },
                tangemMenuUM(isSelectable = true) {
                    item(label = stringReference("By balance"), isSelected = true) {}
                    item(label = stringReference("Manually")) {}
                    divider(title = stringReference("Group"))
                    item(label = stringReference("By network"), iconStart = Icons.ic_eye_20) {}
                    divider()
                    item(label = stringReference("Hide balances"), iconStart = Icons.ic_eye_20, isSelected = true) {}
                },
            ),
        )
    }
}

@Composable
private fun PreviewMenus(states: List<TangemMenu.UiState>) {
    Column(
        modifier = Modifier
            .background(TangemTheme.colors3.bg.primary)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        states.forEach { state ->
            TangemMenuPanel(isSelectable = state.isSelectable, variant = state.variant) {
                TangemMenuEntries(entries = state.entries, onDismissRequest = {})
            }
        }
    }
}
// endregion