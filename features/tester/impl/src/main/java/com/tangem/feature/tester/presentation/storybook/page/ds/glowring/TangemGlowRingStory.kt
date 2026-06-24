@file:Suppress("MagicNumber")

package com.tangem.feature.tester.presentation.storybook.page.ds.glowring

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.glowring.TangemGlowRing
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tester.presentation.storybook.entity.TangemGlowRingStory
import com.tangem.feature.tester.presentation.storybook.entity.TangemGlowRingStory.Background

@Composable
internal fun TangemGlowRingStory(state: TangemGlowRingStory, modifier: Modifier = Modifier) {
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
            QualitySelector(selected = state.quality, onSelect = state.onQualityChange)
            BackgroundSelector(selected = state.background, onSelect = state.onBackgroundChange)
            Toggles(state = state)
        }
    }
}

@Composable
private fun PreviewBackground(background: Background, modifier: Modifier = Modifier) {
    when (background) {
        Background.BgPrimary -> Box(modifier.background(TangemTheme.colors3.bg.primary))
        Background.BgSecondary -> Box(modifier.background(TangemTheme.colors3.bg.secondary))
        Background.BgInverse -> Box(modifier.background(TangemTheme.colors3.bg.inverse))
    }
}

@Composable
private fun ComponentPreview(state: TangemGlowRingStory) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp)),
    ) {
        PreviewBackground(
            background = state.background,
            modifier = Modifier.matchParentSize(),
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 48.dp),
        ) {
            TangemGlowRing(
                modifier = Modifier.size(width = 200.dp, height = 120.dp),
                variant = state.variant,
                animated = state.isAnimated,
                quality = state.quality,
            )
        }
    }
}

@Composable
private fun VariantSelector(selected: TangemGlowRing.Variant, onSelect: (TangemGlowRing.Variant) -> Unit) {
    Section(label = "Variant") {
        ChipGrid(
            items = TangemGlowRing.Variant.entries,
            label = { it.name },
            isSelected = { it == selected },
            onSelect = onSelect,
        )
    }
}

@Composable
private fun QualitySelector(selected: TangemGlowRing.Quality, onSelect: (TangemGlowRing.Quality) -> Unit) {
    Section(label = "Quality (renderer)") {
        ChipGrid(
            items = TangemGlowRing.Quality.entries,
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
private fun Toggles(state: TangemGlowRingStory) {
    Section(label = "Flags") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ToggleRow(label = "animated", checked = state.isAnimated, onToggle = state.onAnimatedToggle)
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