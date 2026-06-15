@file:Suppress("MagicNumber")

package com.tangem.feature.tester.presentation.storybook.page.ds.fade

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.ds2.fade.TangemFade
import com.tangem.core.ui.res.LocalHazeState
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tester.presentation.storybook.entity.TangemFadeStory

@Composable
internal fun TangemFadeStory(state: TangemFadeStory, modifier: Modifier = Modifier) {
    val hazeState = LocalHazeState.current
    DisposableEffect(state.isBlur) {
        val wasBlurEnabled = hazeState.blurEnabled
        hazeState.blurEnabled = state.isBlur
        onDispose { hazeState.blurEnabled = wasBlurEnabled }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors.background.primary)
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ComponentPreview(state = state)
        VariantSelector(selected = state.variant, onSelect = state.onVariantChange)
        PositionSelector(selected = state.position, onSelect = state.onPositionChange)
        Toggles(state = state)
    }
}

@Composable
private fun ComponentPreview(state: TangemFadeStory) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(240.dp)
            .clip(RoundedCornerShape(16.dp)),
    ) {
        Backdrop(
            modifier = Modifier
                .fillMaxSize()
                .hazeSourceTangem(zIndex = 0f),
        )
        TangemFade(
            position = state.position,
            variant = state.variant,
            blur = state.isBlur,
            modifier = Modifier.align(
                when (state.position) {
                    TangemFade.Position.Top -> Alignment.TopCenter
                    TangemFade.Position.Bottom -> Alignment.BottomCenter
                },
            ),
        )
    }
}

@Composable
private fun Backdrop(modifier: Modifier = Modifier) {
    val bands = remember {
        listOf(
            Color(0xFFFF1744),
            Color(0xFFFF9100),
            Color(0xFFFFEA00),
            Color(0xFF00E676),
            Color(0xFF00B8D4),
            Color(0xFF2962FF),
            Color(0xFFD500F9),
        )
    }
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
    val tilePx = with(LocalDensity.current) { 160.dp.toPx() }
    Box(
        modifier = modifier.background(
            brush = Brush.linearGradient(
                colorStops = stops,
                start = Offset(0f, 0f),
                end = Offset(tilePx, tilePx),
                tileMode = TileMode.Repeated,
            ),
        ),
    )
}

@Composable
private fun VariantSelector(selected: TangemFade.Variant, onSelect: (TangemFade.Variant) -> Unit) {
    Section(label = "Variant") {
        ChipGrid(
            items = TangemFade.Variant.entries,
            label = { it.name },
            isSelected = { it == selected },
            onSelect = onSelect,
        )
    }
}

@Composable
private fun PositionSelector(selected: TangemFade.Position, onSelect: (TangemFade.Position) -> Unit) {
    Section(label = "Position") {
        ChipGrid(
            items = TangemFade.Position.entries,
            label = { it.name },
            isSelected = { it == selected },
            onSelect = onSelect,
        )
    }
}

@Composable
private fun Toggles(state: TangemFadeStory) {
    Section(label = "Flags") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ToggleRow(label = "blur", checked = state.isBlur, onToggle = state.onBlurToggle)
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
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors.text.primary1,
        )
        Text(
            text = if (checked) "ON" else "OFF",
            style = TangemTheme.typography.caption2,
            color = if (checked) TangemTheme.colors.text.accent else TangemTheme.colors.text.secondary,
        )
    }
}