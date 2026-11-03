@file:Suppress("MagicNumber")

package com.tangem.feature.tester.presentation.storybook.page.ds.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.menu.TangemMenu
import com.tangem.core.ui.ds2.menu.tangemMenuUi
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_chevron_expand_20
import com.tangem.core.ui.res.generated.icons.ic_chevron_right_20
import com.tangem.core.ui.res.generated.icons.ic_copy_20
import com.tangem.core.ui.res.generated.icons.ic_edit_20
import com.tangem.core.ui.res.generated.icons.ic_share_android_20
import com.tangem.core.ui.res.generated.icons.ic_trash_bin_20
import com.tangem.feature.tester.presentation.storybook.entity.TangemMenuStory

@Composable
internal fun TangemMenuStory(state: TangemMenuStory, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            // The menu is a popup over the whole page, so the whole page is what its material blurs. The source goes
            // before the background: a backdrop recorded without it is transparent and the sharp page shows through.
            .hazeSourceTangem()
            .background(TangemTheme.colors3.bg.primary)
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ComponentPreview(state = state)
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            VariantSelector(selected = state.variant, onSelect = state.onVariantChange)
            LastItemVariantSelector(selected = state.lastItemVariant, onSelect = state.onLastItemVariantChange)
            Toggles(state = state)
        }
    }
}

@Composable
private fun ComponentPreview(state: TangemMenuStory) {
    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(StripesBrush)
            .padding(top = 24.dp),
    ) {
        val onAnchorClick = { state.onExpandedChange(!state.isExpanded) }
        val menu = @Composable {
            TangemMenu(
                state = buildMenuState(state),
                isExpanded = state.isExpanded,
                onDismissRequest = { state.onExpandedChange(false) },
            )
        }
        when (state.variant) {
            TangemMenu.Variant.Button -> Box {
                TangemButton(
                    text = stringReference(if (state.isExpanded) "Close menu" else "Open menu"),
                    variant = TangemButton.Variant.Secondary,
                    onClick = onAnchorClick,
                )
                menu()
            }
            TangemMenu.Variant.Row -> AnchorRow(onClick = onAnchorClick, menu = menu)
        }
    }
}

/** The row's value is the menu anchor: the menu opens right below it, not below the whole row. */
@Composable
private fun AnchorRow(onClick: () -> Unit, menu: @Composable () -> Unit) {
    TangemRow(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(TangemTheme.colors3.bg.primary),
        verticalAlignment = TangemRowVerticalAlignment.Center,
        titleSlot = { TangemRowText(text = "Network", role = TangemRowTextRole.Title) },
        valueSlot = {
            Box {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TangemRowText(text = "Ethereum", role = TangemRowTextRole.Value)
                    Icon(
                        imageVector = Icons.ic_chevron_expand_20,
                        contentDescription = null,
                        tint = TangemTheme.colors3.icon.secondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                menu()
            }
        },
        onClick = onClick,
    )
}

/** Hard-edged color stripes: a flat backdrop would hide the blur. */
private val StripesBrush = Brush.linearGradient(
    colorStops = listOf(
        Color(0xFFFF1744),
        Color(0xFFFFEA00),
        Color(0xFF00E676),
        Color(0xFF2962FF),
        Color(0xFFD500F9),
    ).flatMapIndexed { index, color -> listOf(index / 5f to color, (index + 1) / 5f to color) }.toTypedArray(),
    start = Offset.Zero,
    end = Offset(x = 400f, y = 0f),
    tileMode = TileMode.Repeated,
)

private fun buildMenuState(state: TangemMenuStory): TangemMenu.UiState {
    val labels = listOf("Copy address", "Share", "Rename")
    val icons = listOf(Icons.ic_copy_20, Icons.ic_share_android_20, Icons.ic_edit_20)
    val descriptions = listOf("0x71C7…976F", "Send to a contact", "Visible only to you")

    return tangemMenuUi(variant = state.variant, isSelectable = state.isSelectable) {
        labels.forEachIndexed { index, label ->
            item(
                label = stringReference(label),
                description = if (state.hasDescription) stringReference(descriptions[index]) else null,
                iconStart = if (state.hasIconStart) icons[index] else null,
                iconEnd = if (state.hasIconEnd) Icons.ic_chevron_right_20 else null,
                isSelected = state.selectedIndex == index,
                isDismissOnClick = !state.isSelectable,
            ) { state.onItemSelect(index) }
        }
        if (state.hasDivider) {
            divider(title = if (state.hasDividerTitle) stringReference("Danger zone") else null)
        }
        item(
            label = stringReference("Delete wallet"),
            description = if (state.hasDescription) stringReference("This can't be undone") else null,
            iconStart = if (state.hasIconStart) Icons.ic_trash_bin_20 else null,
            iconEnd = if (state.hasIconEnd) Icons.ic_chevron_right_20 else null,
            variant = state.lastItemVariant,
            isEnabled = state.isLastItemEnabled,
            isSelected = state.selectedIndex == labels.size,
            isDismissOnClick = !state.isSelectable,
        ) { state.onItemSelect(labels.size) }
    }
}

@Composable
private fun VariantSelector(selected: TangemMenu.Variant, onSelect: (TangemMenu.Variant) -> Unit) {
    Section(label = "Variant (anchor)") {
        ChipGrid(
            items = TangemMenu.Variant.entries,
            label = { it.name },
            isSelected = { it == selected },
            onSelect = onSelect,
        )
    }
}

@Composable
private fun LastItemVariantSelector(selected: TangemMenu.ItemVariant, onSelect: (TangemMenu.ItemVariant) -> Unit) {
    Section(label = "Last item variant") {
        ChipGrid(
            items = TangemMenu.ItemVariant.entries,
            label = { it.name },
            isSelected = { it == selected },
            onSelect = onSelect,
        )
    }
}

@Composable
private fun Toggles(state: TangemMenuStory) {
    Section(label = "Flags") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ToggleRow(label = "isSelectable", checked = state.isSelectable, onToggle = state.onSelectableToggle)
            ToggleRow(label = "iconStart", checked = state.hasIconStart, onToggle = state.onIconStartToggle)
            ToggleRow(label = "iconEnd", checked = state.hasIconEnd, onToggle = state.onIconEndToggle)
            ToggleRow(label = "description", checked = state.hasDescription, onToggle = state.onDescriptionToggle)
            ToggleRow(label = "divider", checked = state.hasDivider, onToggle = state.onDividerToggle)
            ToggleRow(label = "divider title", checked = state.hasDividerTitle, onToggle = state.onDividerTitleToggle)
            ToggleRow(
                label = "last item isEnabled",
                checked = state.isLastItemEnabled,
                onToggle = state.onLastItemEnabledToggle,
            )
        }
    }
}

@Composable
private fun Section(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = label,
            style = TangemTheme.typography.subtitle1,
            color = TangemTheme.colors3.text.primary,
        )
        content()
    }
}

@Composable
private fun <T> ChipGrid(items: List<T>, label: (T) -> String, isSelected: (T) -> Boolean, onSelect: (T) -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(shape)
            .background(TangemTheme.colors3.bg.primary)
            .border(width = 1.dp, color = TangemTheme.colors3.border.tertiary, shape = shape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items.forEach { item ->
            Chip(
                label = label(item),
                selected = isSelected(item),
                onClick = { onSelect(item) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) TangemTheme.colors3.bg.secondary else TangemTheme.colors3.bg.primary)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
    ) {
        Text(
            text = label,
            style = TangemTheme.typography.caption2,
            color = if (selected) TangemTheme.colors3.text.primary else TangemTheme.colors3.text.secondary,
        )
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TangemTheme.colors3.bg.primary)
            .clickable(onClick = onToggle)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors3.text.primary,
        )
        Text(
            text = if (checked) "ON" else "OFF",
            style = TangemTheme.typography.caption2,
            color = if (checked) TangemTheme.colors3.text.accent.blue else TangemTheme.colors3.text.secondary,
        )
    }
}