@file:Suppress("MagicNumber", "LongMethod")

package com.tangem.feature.tester.presentation.storybook.page.badge

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.ds.badge.*
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tester.presentation.storybook.entity.TangemBadgeStory

private const val STATE_LABEL_WIDTH = 80

@Composable
internal fun TangemBadgeStory(state: TangemBadgeStory, modifier: Modifier = Modifier) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .statusBarsPadding()
            .fillMaxSize()
            .background(TangemTheme.colors2.surface.level1),
    ) {
        stickyHeader("color_toggle") {
            ColorToggle(
                selected = state.selectedColor,
                onSelect = state.onColorChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TangemTheme.colors2.surface.level1)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        TangemBadgeSize.entries.forEach { size ->
            item(size.name) {
                BadgeSizeSection(size = size, color = state.selectedColor)
            }
        }
    }
}

@Composable
private fun ColorToggle(
    selected: TangemBadgeColor,
    onSelect: (TangemBadgeColor) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .clip(shape)
            .background(TangemTheme.colors2.surface.level2)
            .border(width = 1.dp, color = TangemTheme.colors2.border.neutral.secondary, shape = shape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TangemBadgeColor.entries.forEach { color ->
            ColorChip(
                label = color.name,
                selected = color == selected,
                onClick = { onSelect(color) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ColorChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val chipShape = RoundedCornerShape(50)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(chipShape)
            .background(if (selected) TangemTheme.colors2.surface.level3 else TangemTheme.colors2.surface.level2)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
    ) {
        Text(
            text = label,
            style = TangemTheme.typography.caption2,
            color = if (selected) TangemTheme.colors.text.primary1 else TangemTheme.colors.text.secondary,
        )
    }
}

@Composable
private fun BadgeSizeSection(size: TangemBadgeSize, color: TangemBadgeColor) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(TangemTheme.colors2.surface.level1)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = size.name,
            style = TangemTheme.typography.subtitle1,
            color = TangemTheme.colors.text.primary1,
        )
        TangemBadgeShape.entries.forEach { shape ->
            BadgeShapeGroup(size = size, shape = shape, color = color)
        }
    }
    HorizontalDivider(
        color = TangemTheme.colors2.border.neutral.secondary,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

@Composable
private fun BadgeShapeGroup(size: TangemBadgeSize, shape: TangemBadgeShape, color: TangemBadgeColor) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = shape.name,
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.secondary,
        )
        ColumnHeaderRow()
        TangemBadgeType.entries.forEach { type ->
            BadgeTypeRow(size = size, shape = shape, color = color, type = type)
        }
    }
}

@Composable
private fun ColumnHeaderRow() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(STATE_LABEL_WIDTH.dp))
        Text(
            text = "Text + Icon",
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.tertiary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "Text",
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.tertiary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "Icon",
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.tertiary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun BadgeTypeRow(
    size: TangemBadgeSize,
    shape: TangemBadgeShape,
    color: TangemBadgeColor,
    type: TangemBadgeType,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = type.name,
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.tertiary,
            modifier = Modifier.width(STATE_LABEL_WIDTH.dp),
        )
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier.weight(1f),
        ) {
            TangemBadge(
                text = stringReference("New"),
                tangemIconUM = TangemIconUM.Icon(iconRes = R.drawable.ic_information_24),
                size = size,
                shape = shape,
                color = color,
                type = type,
                iconPosition = TangemBadgeIconPosition.Start,
                onClick = {},
            )
        }
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier.weight(1f),
        ) {
            TangemBadge(
                text = stringReference("New"),
                size = size,
                shape = shape,
                color = color,
                type = type,
                onClick = {},
            )
        }
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier.weight(1f),
        ) {
            TangemBadge(
                tangemIconUM = TangemIconUM.Icon(R.drawable.ic_information_24),
                size = size,
                shape = shape,
                color = color,
                type = type,
                onClick = {},
            )
        }
    }
}