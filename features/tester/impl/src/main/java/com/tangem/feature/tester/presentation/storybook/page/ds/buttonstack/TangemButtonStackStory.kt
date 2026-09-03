@file:Suppress("MagicNumber")

package com.tangem.feature.tester.presentation.storybook.page.ds.buttonstack

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.GroupEntry
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.button.TangemButtonStack
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.LocalHazeState
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tester.presentation.storybook.entity.TangemButtonStackStory
import com.tangem.feature.tester.presentation.storybook.entity.TangemButtonStackStory.Background
import com.tangem.feature.tester.presentation.storybook.entity.TangemButtonStackStory.EntryKind

private const val MAX_ENTRY_COUNT = 4

private val EntryLabels = listOf("Buy", "Sell", "Swap", "Stake")

@Composable
internal fun TangemButtonStackStory(state: TangemButtonStackStory, modifier: Modifier = Modifier) {
    val hazeState = LocalHazeState.current
    SideEffect { hazeState.blurEnabled = state.isBlurEnabled }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors.background.primary)
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ComponentPreview(state = state)
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            VariantSelector(selected = state.variant, onSelect = state.onVariantChange)
            EntryCountSelector(selected = state.entryCount, onSelect = state.onEntryCountChange)
            EntryKindSelector(selected = state.entryKind, onSelect = state.onEntryKindChange)
            BackgroundSelector(selected = state.background, onSelect = state.onBackgroundChange)
            Toggles(state = state)
            Hint()
        }
    }
}

@Composable
private fun ComponentPreview(state: TangemButtonStackStory) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp)),
    ) {
        PreviewBackground(
            background = state.background,
            modifier = Modifier
                .matchParentSize()
                .hazeSourceTangem(zIndex = -1f),
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
        ) {
            TangemButtonStack(variant = state.variant) {
                repeat(times = state.entryCount) { index ->
                    StackEntry(kind = state.entryKind, label = EntryLabels[index])
                }
            }
        }
    }
}

@Composable
private fun StackEntry(kind: EntryKind, label: String) {
    val icon = TangemIconUM.Icon(iconRes = R.drawable.ic_information_24)
    when (kind) {
        EntryKind.Icon -> TangemButton.GroupEntry(iconUM = icon, onClick = {})
        EntryKind.Label -> TangemButton(
            variant = TangemButton.Variant.Ghost,
            size = TangemButton.Size.X9,
            text = stringReference(label),
            onClick = {},
        )
        EntryKind.IconLabel -> TangemButton(
            variant = TangemButton.Variant.Ghost,
            size = TangemButton.Size.X9,
            iconStart = icon,
            text = stringReference(label),
            onClick = {},
        )
    }
}

@Composable
private fun VariantSelector(selected: TangemButtonStack.Variant, onSelect: (TangemButtonStack.Variant) -> Unit) {
    Section(label = "Variant") {
        ChipGrid(
            items = TangemButtonStack.Variant.entries,
            label = { it.name },
            isSelected = { it == selected },
            onSelect = onSelect,
        )
    }
}

@Composable
private fun EntryCountSelector(selected: Int, onSelect: (Int) -> Unit) {
    Section(label = "Entries") {
        ChipGrid(
            items = (1..MAX_ENTRY_COUNT).toList(),
            label = { it.toString() },
            isSelected = { it == selected },
            onSelect = onSelect,
        )
    }
}

@Composable
private fun EntryKindSelector(selected: EntryKind, onSelect: (EntryKind) -> Unit) {
    Section(label = "Entry") {
        ChipGrid(
            items = EntryKind.entries,
            label = { it.label },
            isSelected = { it == selected },
            onSelect = onSelect,
        )
    }
}

@Composable
private fun BackgroundSelector(selected: Background, onSelect: (Background) -> Unit) {
    Section(label = "Background") {
        ChipGrid(
            items = Background.entries,
            label = { it.label },
            isSelected = { it == selected },
            onSelect = onSelect,
        )
    }
}

@Composable
private fun Toggles(state: TangemButtonStackStory) {
    Section(label = "Flags") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ToggleRow(label = "blur", checked = state.isBlurEnabled, onToggle = state.onBlurToggle)
        }
    }
}

@Composable
private fun Hint() {
    Text(
        modifier = Modifier.padding(horizontal = 16.dp),
        text = "Switch the variant to see the material pill cross-fade in and out while the entries " +
            "stay put. The same stack is the secondary-actions group of the top navigation bar.",
        style = TangemTheme.typography.caption2,
        color = TangemTheme.colors.text.secondary,
    )
}

@Composable
private fun PreviewBackground(background: Background, modifier: Modifier = Modifier) {
    when (background) {
        Background.Rainbow -> BlurTestBackground(modifier = modifier)
        Background.BgPrimary -> Box(modifier.background(TangemTheme.colors3.bg.primary))
        Background.BgSecondary -> Box(modifier.background(TangemTheme.colors3.bg.secondary))
        Background.BgBrand -> Box(modifier.background(TangemTheme.colors3.bg.brand))
        Background.BgInverse -> Box(modifier.background(TangemTheme.colors3.bg.inverse))
    }
}

@Composable
private fun BlurTestBackground(modifier: Modifier = Modifier) {
    val bands = remember {
        listOf(
            Color(0xFFFF1744), // red
            Color(0xFFFF9100), // orange
            Color(0xFFFFEA00), // yellow
            Color(0xFF00E676), // green
            Color(0xFF00B8D4), // cyan
            Color(0xFF2962FF), // blue
            Color(0xFFD500F9), // magenta
        )
    }
    // Hard-edged stripes — sharp seams make the blur visually obvious.
    val stops = remember(bands) {
        buildList {
            bands.forEachIndexed { index, color ->
                val start = index.toFloat() / bands.size
                val end = (index + 1).toFloat() / bands.size
                add(start to color)
                add(end to color)
            }
        }.toTypedArray()
    }
    val tilePx = with(LocalDensity.current) { 320.dp.toPx() }
    val transition = rememberInfiniteTransition(label = "blur-bg")
    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = tilePx,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "blur-bg-offset",
    )
    Box(
        modifier = modifier.background(
            brush = Brush.linearGradient(
                colorStops = stops,
                start = Offset(offset, 0f),
                end = Offset(offset + tilePx, 0f),
                tileMode = TileMode.Repeated,
            ),
        ),
    )
}

@Composable
private fun Section(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = label,
            style = TangemTheme.typography.subtitle1,
            color = TangemTheme.colors.text.primary1,
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
            .background(TangemTheme.colors2.surface.level2)
            .border(
                width = 1.dp,
                color = TangemTheme.colors2.border.neutral.secondary,
                shape = shape,
            )
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
    val chipShape = RoundedCornerShape(50)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(chipShape)
            .background(
                if (selected) TangemTheme.colors2.surface.level3 else TangemTheme.colors2.surface.level2,
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
    ) {
        Text(
            text = label,
            maxLines = 1,
            style = TangemTheme.typography.caption2,
            color = if (selected) TangemTheme.colors.text.primary1 else TangemTheme.colors.text.secondary,
        )
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TangemTheme.colors2.surface.level2)
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.primary1,
        )
        Text(
            text = if (checked) "ON" else "OFF",
            style = TangemTheme.typography.caption2,
            color = if (checked) TangemTheme.colors.text.accent else TangemTheme.colors.text.secondary,
        )
    }
}