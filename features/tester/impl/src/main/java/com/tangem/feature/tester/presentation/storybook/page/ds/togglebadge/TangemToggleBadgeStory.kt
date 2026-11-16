@file:Suppress("MagicNumber")

package com.tangem.feature.tester.presentation.storybook.page.ds.togglebadge

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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.togglebadge.TangemToggleBadge
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tester.presentation.storybook.entity.TangemToggleBadgeStory

@Composable
internal fun TangemToggleBadgeStory(state: TangemToggleBadgeStory, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors3.bg.primary)
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ComponentPreview(state = state)
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SizeSelector(selected = state.size, onSelect = state.onSizeChange)
            TextScaleSlider(value = state.textScale, onChange = state.onTextScaleChange)
            Toggles(state = state)
        }
    }
}

@Composable
private fun ComponentPreview(state: TangemToggleBadgeStory) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(TangemTheme.colors3.bg.secondary)
            .padding(vertical = 32.dp),
    ) {
        val baseDensity = LocalDensity.current
        val scaledDensity = remember(baseDensity, state.textScale) {
            Density(density = baseDensity.density, fontScale = state.textScale)
        }
        val icon = remember { TangemIconUM.Icon(iconRes = R.drawable.ic_information_24) }
        CompositionLocalProvider(LocalDensity provides scaledDensity) {
            TangemToggleBadge(
                text = stringReference("Favorites"),
                isSelected = state.isSelected,
                onSelectedChange = state.onSelectedChange,
                size = state.size,
                iconStart = icon.takeIf { state.hasIconStart },
                iconEnd = icon.takeIf { state.hasIconEnd },
                enabled = state.isEnabled,
            )
        }
    }
}

@Composable
private fun SizeSelector(selected: TangemBadge.Size, onSelect: (TangemBadge.Size) -> Unit) {
    Section(label = "Size") {
        ChipGrid(
            items = TangemBadge.Size.entries,
            label = { it.name },
            isSelected = { it == selected },
            onSelect = onSelect,
        )
    }
}

@Composable
private fun TextScaleSlider(value: Float, onChange: (Float) -> Unit) {
    Section(label = "Text scale: ${"%.2f".format(value)}x") {
        Slider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            value = value,
            onValueChange = onChange,
            valueRange = 0.5f..2f,
            steps = 14,
            colors = SliderDefaults.colors(
                thumbColor = TangemTheme.colors3.text.accent.blue,
                activeTrackColor = TangemTheme.colors3.text.accent.blue,
                activeTickColor = TangemTheme.colors3.bg.secondary,
                inactiveTrackColor = TangemTheme.colors3.bg.secondary,
                inactiveTickColor = TangemTheme.colors3.text.accent.blue,
            ),
        )
    }
}

@Composable
private fun Toggles(state: TangemToggleBadgeStory) {
    Section(label = "Flags") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ToggleRow(
                label = "isSelected",
                checked = state.isSelected,
                onToggle = { state.onSelectedChange(!state.isSelected) },
            )
            ToggleRow(label = "enabled", checked = state.isEnabled, onToggle = state.onEnabledToggle)
            ToggleRow(label = "iconStart", checked = state.hasIconStart, onToggle = state.onIconStartToggle)
            ToggleRow(label = "iconEnd", checked = state.hasIconEnd, onToggle = state.onIconEndToggle)
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
            .border(
                width = 1.dp,
                color = TangemTheme.colors3.border.tertiary,
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
                if (selected) TangemTheme.colors3.bg.secondary else TangemTheme.colors3.bg.primary,
            )
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
            .padding(horizontal = 12.dp, vertical = 12.dp),
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