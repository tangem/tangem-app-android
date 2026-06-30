@file:Suppress("MagicNumber")

package com.tangem.feature.tester.presentation.storybook.page.ds.button

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.LocalHazeState
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tester.presentation.storybook.entity.TangemButtonStory
import com.tangem.feature.tester.presentation.storybook.entity.TangemButtonStory.Background

@Composable
internal fun TangemButtonStory(state: TangemButtonStory, modifier: Modifier = Modifier) {
    val hazeState = LocalHazeState.current
    SideEffect { hazeState.blurEnabled = state.isBlurEnabled }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors.background.primary)
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Preview stays pinned at the top.
        ComponentPreview(state = state)
        // Only the controls scroll.
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            VariantSelector(selected = state.variant, onSelect = state.onVariantChange)
            SizeSelector(selected = state.size, onSelect = state.onSizeChange)
            BackgroundSelector(selected = state.background, onSelect = state.onBackgroundChange)
            TextScaleSlider(value = state.textScale, onChange = state.onTextScaleChange)
            Toggles(state = state)
        }
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
private fun ComponentPreview(state: TangemButtonStory) {
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
            val baseDensity = LocalDensity.current
            val scaledDensity = remember(baseDensity, state.textScale) {
                Density(density = baseDensity.density, fontScale = state.textScale)
            }
            CompositionLocalProvider(LocalDensity provides scaledDensity) {
                TangemButton(
                    variant = state.variant,
                    size = state.size,
                    isLoading = state.isLoading,
                    isEnabled = state.isEnabled,
                    iconStart = if (state.hasIconStart) {
                        TangemIconUM.Icon(iconRes = R.drawable.ic_information_24)
                    } else {
                        null
                    },
                    iconEnd = if (state.hasIconEnd) {
                        TangemIconUM.Icon(iconRes = R.drawable.ic_information_24)
                    } else {
                        null
                    },
                    text = if (state.hasText) stringReference("Button") else null,
                    onClick = {},
                )
            }
        }
    }
}

@Composable
private fun VariantSelector(selected: TangemButton.Variant, onSelect: (TangemButton.Variant) -> Unit) {
    Section(label = "Variant") {
        ChipGrid(
            items = TangemButton.Variant.entries,
            label = { it.name },
            isSelected = { it == selected },
            onSelect = onSelect,
        )
    }
}

@Composable
private fun SizeSelector(selected: TangemButton.Size, onSelect: (TangemButton.Size) -> Unit) {
    Section(label = "Size") {
        ChipGrid(
            items = TangemButton.Size.entries,
            label = { it.name },
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
                thumbColor = TangemTheme.colors.text.accent,
                activeTrackColor = TangemTheme.colors.text.accent,
                activeTickColor = TangemTheme.colors2.surface.level3,
                inactiveTrackColor = TangemTheme.colors2.surface.level3,
                inactiveTickColor = TangemTheme.colors.text.accent,
            ),
        )
    }
}

@Composable
private fun Toggles(state: TangemButtonStory) {
    Section(label = "Flags") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ToggleRow(label = "isLoading", checked = state.isLoading, onToggle = state.onLoadingToggle)
            ToggleRow(label = "isEnabled", checked = state.isEnabled, onToggle = state.onEnabledToggle)
            ToggleRow(label = "iconStart", checked = state.hasIconStart, onToggle = state.onIconStartToggle)
            ToggleRow(label = "iconEnd", checked = state.hasIconEnd, onToggle = state.onIconEndToggle)
            ToggleRow(label = "text", checked = state.hasText, onToggle = state.onTextToggle)
            ToggleRow(label = "blur", checked = state.isBlurEnabled, onToggle = state.onBlurToggle)
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