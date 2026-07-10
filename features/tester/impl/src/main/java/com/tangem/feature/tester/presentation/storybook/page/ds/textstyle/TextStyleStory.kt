@file:Suppress("MagicNumber")

package com.tangem.feature.tester.presentation.storybook.page.ds.textstyle

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tester.presentation.storybook.entity.TextStyleStory

@Composable
internal fun TextStyleStory(state: TextStyleStory, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .statusBarsPadding()
            .fillMaxSize()
            .background(TangemTheme.colors2.surface.level1),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        ComponentPreview(style = state.style, textScale = state.textScale)
        StyleSelector(selected = state.style, onSelect = state.onStyleChange)
        TextScaleSlider(value = state.textScale, onChange = state.onTextScaleChange)
    }
}

@Composable
private fun ComponentPreview(style: TextStyleStory.Style, textScale: Float) {
    val textStyle = style.toTextStyle()
    val baseDensity = LocalDensity.current
    val scaledDensity = remember(baseDensity, textScale) {
        Density(density = baseDensity.density, fontScale = textScale)
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(TangemTheme.colors2.surface.level2)
            .padding(24.dp),
    ) {
        CompositionLocalProvider(LocalDensity provides scaledDensity) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                SampleText(text = "multiline\nstring", style = textStyle)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SampleText(text = "one string", style = textStyle)
                    SampleText(text = "separate string", style = textStyle)
                }
            }
        }
    }
}

@Composable
private fun SampleText(text: String, style: TextStyle) {
    Text(
        text = text,
        style = style,
        color = TangemTheme.colors3.text.primary,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun StyleSelector(selected: TextStyleStory.Style, onSelect: (TextStyleStory.Style) -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = "Text style",
            style = TangemTheme.typography.subtitle1,
            color = TangemTheme.colors.text.primary1,
        )
        val shape = RoundedCornerShape(50)
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
            TextStyleStory.Style.entries.forEach { style ->
                StyleChip(
                    label = style.label,
                    selected = style == selected,
                    onClick = { onSelect(style) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TextScaleSlider(value: Float, onChange: (Float) -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = "Text scale: ${"%.2f".format(value)}x",
            style = TangemTheme.typography.subtitle1,
            color = TangemTheme.colors.text.primary1,
        )
        Slider(
            modifier = Modifier.fillMaxWidth(),
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
private fun StyleChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
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
@ReadOnlyComposable
private fun TextStyleStory.Style.toTextStyle(): TextStyle = when (this) {
    TextStyleStory.Style.Display -> TangemTheme.typography3.display.medium
    TextStyleStory.Style.HeadM -> TangemTheme.typography3.heading.medium
    TextStyleStory.Style.HeadS -> TangemTheme.typography3.heading.small
    TextStyleStory.Style.Body -> TangemTheme.typography3.body.medium
    TextStyleStory.Style.SubH -> TangemTheme.typography3.subheading.medium
    TextStyleStory.Style.Caption -> TangemTheme.typography3.caption.medium
}