@file:Suppress("MagicNumber")

package com.tangem.feature.tester.presentation.storybook.page.ds.shtorka

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.ds2.shtorka.TangemShtorka
import com.tangem.core.ui.ds2.shtorka.TangemShtorkaState
import com.tangem.core.ui.ds2.shtorka.rememberTangemShtorkaState
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.LocalHazeState
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tester.presentation.storybook.entity.TangemShtorkaStory
import com.tangem.feature.tester.presentation.storybook.page.ds.TransparentSystemBarsEffect

// The collapsed detent shows exactly the grabber + top navigation header.
private val ShtorkaDetents = listOf(
    TangemShtorka.Detent.Height(value = 96.dp),
    TangemShtorka.Detent.Fraction(value = 0.5f),
    TangemShtorka.Detent.Full,
)

private val DetentLabels = mapOf<TangemShtorka.Detent, String>(
    ShtorkaDetents[0] to "Peek",
    ShtorkaDetents[1] to "Half",
    ShtorkaDetents[2] to "Full",
)

@Composable
internal fun TangemShtorkaStory(state: TangemShtorkaStory, modifier: Modifier = Modifier) {
    val hazeState = LocalHazeState.current
    SideEffect { hazeState.blurEnabled = state.isBlurEnabled }

    val shtorkaState = rememberTangemShtorkaState(
        detents = ShtorkaDetents,
        initialDetent = ShtorkaDetents[1],
    )

    TransparentSystemBarsEffect()

    Box(modifier = modifier.fillMaxSize()) {
        FakeMapBackground(
            modifier = Modifier
                .fillMaxSize()
                .hazeSourceTangem(zIndex = -1f),
        )
        Controls(
            state = state,
            shtorkaState = shtorkaState,
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp),
        )
        TangemShtorka(
            state = shtorkaState,
            isMaterial = state.isMaterial,
            materialStyle = state.materialStyle,
            showDragHandle = state.showDragHandle,
            dragHandleContentDescription = "Drag handle",
            header = {
                TangemTopNavigation(
                    title = stringReference("Title"),
                    subtitle = stringReference("Subtitle"),
                    contentAlign = TangemTopNavigation.ContentAlign.Center,
                    windowInsets = WindowInsets(0.dp),
                    fadeEnabled = false,
                    onBack = {},
                    onClose = {},
                )
            },
        ) {
            if (state.isContentScrollable) {
                ScrollablePlaces()
            } else {
                StaticPlaces()
            }
        }
    }
}

// region shtorka content

// Grabber (16dp) + top navigation (~72dp) overlay height the content starts below.
private val HeaderHeight = 88.dp

@Composable
private fun ScrollablePlaces() {
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = HeaderHeight, end = 16.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(count = 40, key = { it }) { index -> PlaceRow(index = index) }
    }
}

@Composable
private fun StaticPlaces() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = HeaderHeight)
            .padding(horizontal = 16.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(times = 5) { index -> PlaceRow(index = index) }
    }
}

@Composable
private fun PlaceRow(index: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = {})
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = "Coffee & Co. ${index + 1}",
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.primary,
        )
        Text(
            text = "${(index + 3) * 90} m away · Open until 22:00",
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
        )
    }
}

// endregion

// region controls

@Composable
private fun Controls(state: TangemShtorkaStory, shtorkaState: TangemShtorkaState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(TangemTheme.colors2.surface.level1)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DetentSelector(shtorkaState = shtorkaState)
        MaterialStyleSelector(selected = state.materialStyle, onSelect = state.onMaterialStyleChange)
        ToggleRow(label = "isMaterial", checked = state.isMaterial, onToggle = state.onMaterialToggle)
        ToggleRow(label = "blur", checked = state.isBlurEnabled, onToggle = state.onBlurToggle)
        ToggleRow(label = "dragHandle", checked = state.showDragHandle, onToggle = state.onDragHandleToggle)
        ToggleRow(
            label = "scrollable content",
            checked = state.isContentScrollable,
            onToggle = state.onContentScrollableToggle,
        )
    }
}

@Composable
private fun DetentSelector(shtorkaState: TangemShtorkaState) {
    ChipRow(
        items = ShtorkaDetents,
        label = { DetentLabels.getValue(it) },
        isSelected = { it == shtorkaState.targetDetent },
        onSelect = { shtorkaState.animateTo(it) },
    )
}

@Composable
private fun MaterialStyleSelector(
    selected: TangemSurface.MaterialStyle,
    onSelect: (TangemSurface.MaterialStyle) -> Unit,
) {
    ChipRow(
        items = TangemSurface.MaterialStyle.entries,
        label = { it.name },
        isSelected = { it == selected },
        onSelect = onSelect,
    )
}

@Composable
private fun <T> ChipRow(items: List<T>, label: (T) -> String, isSelected: (T) -> Boolean, onSelect: (T) -> Unit) {
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

// endregion

// region backdrop

/** Animated hard-edged color stripes standing in for a map — makes the material blur obvious. */
@Composable
private fun FakeMapBackground(modifier: Modifier = Modifier) {
    val isDark = isSystemInDarkTheme()
    val bands = remember(isDark) {
        if (isDark) {
            listOf(
                Color(0xFF12262E), // water
                Color(0xFF1B2A1E), // park
                Color(0xFF2A2A20), // roads
                Color(0xFF33271F), // buildings
                Color(0xFF16333A),
                Color(0xFF20301F),
            )
        } else {
            listOf(
                Color(0xFFB2DFDB), // water
                Color(0xFFC8E6C9), // park
                Color(0xFFFFF9C4), // roads
                Color(0xFFFFCCBC), // buildings
                Color(0xFF80CBC4),
                Color(0xFFA5D6A7),
            )
        }
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
    val tilePx = with(LocalDensity.current) { 320.dp.toPx() }
    val transition = rememberInfiniteTransition(label = "shtorka-bg")
    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = tilePx,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shtorka-bg-offset",
    )
    Box(
        modifier = modifier.background(
            brush = Brush.linearGradient(
                colorStops = stops,
                start = Offset(offset, 0f),
                end = Offset(offset + tilePx, tilePx),
                tileMode = TileMode.Repeated,
            ),
        ),
    )
}

// endregion