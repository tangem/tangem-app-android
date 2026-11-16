@file:Suppress("MagicNumber")

package com.tangem.feature.tester.presentation.storybook.page.ds.scorescale

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import com.tangem.core.ui.ds2.scorescale.TangemScoreScale
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tester.presentation.storybook.entity.TangemScoreScaleStory
import com.tangem.feature.tester.presentation.storybook.entity.TangemScoreScaleStory.Width

@Composable
internal fun TangemScoreScaleStory(state: TangemScoreScaleStory, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .statusBarsPadding()
            .fillMaxSize()
            .background(TangemTheme.colors3.bg.primary),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        ComponentPreview(state = state)
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            WidthSelector(selected = state.width, onSelect = state.onWidthChange)
            TextScaleSlider(value = state.textScale, onChange = state.onTextScaleChange)
            Section(label = "Flags") {
                ToggleRow(label = "isEnabled", checked = state.isEnabled, onToggle = state.onEnabledToggle)
            }
        }
    }
}

@Composable
private fun ComponentPreview(state: TangemScoreScaleStory) {
    val baseDensity = LocalDensity.current
    val scaledDensity = remember(baseDensity, state.textScale) {
        Density(density = baseDensity.density, fontScale = state.textScale)
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
    ) {
        CompositionLocalProvider(LocalDensity provides scaledDensity) {
            TangemScoreScale(
                selectedScore = state.selectedScore,
                onScoreClick = state.onScoreClick,
                isEnabled = state.isEnabled,
                modifier = state.width.dp?.let { Modifier.width(it.dp) } ?: Modifier.fillMaxWidth(),
            )
        }
    }
}

private val Width.dp: Int?
    get() = when (this) {
        Width.Full -> null
        Width.Phone -> 328
        Width.Narrow -> 280
        Width.Compact -> 200
    }

@Composable
private fun WidthSelector(selected: Width, onSelect: (Width) -> Unit) {
    Section(label = "Width") {
        val shape = RoundedCornerShape(20.dp)
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(shape)
                .border(width = 1.dp, color = TangemTheme.colors3.border.tertiary, shape = shape)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Width.entries.forEach { width ->
                Chip(
                    label = width.dp?.let { "$it dp" } ?: "Full",
                    selected = width == selected,
                    onClick = { onSelect(width) },
                )
            }
        }
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
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) TangemTheme.colors3.bg.secondary else TangemTheme.colors3.bg.primary)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 12.dp),
    ) {
        Text(
            text = label,
            style = TangemTheme.typography.caption2,
            color = if (selected) TangemTheme.colors3.text.primary else TangemTheme.colors3.text.secondary,
            maxLines = 1,
        )
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = TangemTheme.typography.body2, color = TangemTheme.colors3.text.primary)
        Text(
            text = if (checked) "ON" else "OFF",
            style = TangemTheme.typography.caption2,
            color = if (checked) TangemTheme.colors3.text.accent.blue else TangemTheme.colors3.text.secondary,
        )
    }
}