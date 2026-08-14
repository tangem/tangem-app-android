@file:Suppress("MagicNumber")

package com.tangem.feature.tester.presentation.storybook.page.ds.tabnavigation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItem
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.core.ui.ds2.tabnavigation.TangemTabNavigation
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.LocalHazeState
import com.tangem.core.ui.res.LocalIsInDarkTheme
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemeRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_wallet_20
import com.tangem.feature.tester.presentation.storybook.entity.TangemTabNavigationStory
import com.tangem.feature.tester.presentation.storybook.entity.TangemTabNavigationStory.Background
import com.tangem.feature.tester.presentation.storybook.entity.TangemTabNavigationStory.Theme
import kotlinx.collections.immutable.toImmutableList

internal data class DemoTab(val id: String, val label: String, val counter: String)

internal val DEMO_TABS = listOf(
    DemoTab(id = "all", label = "All", counter = "12"),
    DemoTab(id = "etfs", label = "ETFs", counter = "3"),
    DemoTab(id = "funds", label = "Funds", counter = "7"),
    DemoTab(id = "market", label = "Market", counter = "48"),
    DemoTab(id = "staking", label = "Staking", counter = "21"),
    DemoTab(id = "others", label = "Others", counter = "2"),
)

@Composable
internal fun TangemTabNavigationStory(state: TangemTabNavigationStory, modifier: Modifier = Modifier) {
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
            BackgroundSelector(selected = state.background, onSelect = state.onBackgroundChange)
            ThemeSelector(selected = state.theme, onSelect = state.onThemeChange)
            TextScaleSlider(value = state.textScale, onChange = state.onTextScaleChange)
            Toggles(state = state)
            Hint()
        }
    }
}

@Composable
private fun ComponentPreview(state: TangemTabNavigationStory) {
    val isAppDark = LocalIsInDarkTheme.current
    val isPreviewDark = when (state.theme) {
        Theme.Light -> false
        Theme.Dark -> true
        Theme.System -> isAppDark
    }
    // The component has no inverse-content variant in Figma, so on the one backdrop that is defined as
    // the opposite of its theme it flips too — otherwise it draws white labels on white.
    val isComponentDark = when (state.background) {
        Background.BgInverse -> !isPreviewDark
        else -> isPreviewDark
    }

    ThemedAs(isDark = isPreviewDark) {
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
            val baseDensity = LocalDensity.current
            val scaledDensity = remember(baseDensity, state.textScale) {
                Density(density = baseDensity.density, fontScale = state.textScale)
            }
            CompositionLocalProvider(LocalDensity provides scaledDensity) {
                ThemedAs(isDark = isComponentDark) {
                    TabRow(state = state, modifier = Modifier.padding(vertical = 32.dp))
                }
            }
        }
    }
}

@Composable
private fun ThemedAs(isDark: Boolean, content: @Composable () -> Unit) {
    if (isDark == LocalIsInDarkTheme.current) {
        content()
        return
    }

    // `colors3` is provided by TangemThemeRedesign, which picks light/dark off LocalIsInDarkTheme, so
    // flipping that local and re-running it is what actually re-themes a DS3 component. Wrapping in
    // TangemTheme(isDark = …) instead does nothing here: it never touches colors3.
    val hazeState = LocalHazeState.current
    CompositionLocalProvider(LocalIsInDarkTheme provides isDark) {
        TangemThemeRedesign {
            // TangemThemeRedesign installs a fresh HazeState. Restoring the enclosing one keeps the
            // backdrop's haze source and the Material pill's haze effect on the same state, however
            // many of these wrappers they sit inside.
            CompositionLocalProvider(LocalHazeState provides hazeState) {
                content()
            }
        }
    }
}

@Composable
private fun TabRow(state: TangemTabNavigationStory, modifier: Modifier = Modifier) {
    TangemTabNavigation(
        modifier = modifier,
        tabs = DEMO_TABS.map { state.toItem(it) }.toImmutableList(),
        variant = state.variant,
    )
}

private fun TangemTabNavigationStory.toItem(tab: DemoTab): TangemTabItemUM = when {
    isLoading -> TangemTabItemUM.Loading(id = tab.id)
    else -> TangemTabItemUM.Content(
        id = tab.id,
        label = stringReference(tab.label),
        counter = stringReference(tab.counter).takeIf { hasCounter },
        iconStart = TangemIconUM.Icon(Icons.ic_wallet_20).takeIf { hasIcon },
        isSelected = tab.id == selectedTabId,
        onClick = { onTabClick(tab.id) },
    )
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
private fun VariantSelector(selected: TangemTabItem.Variant, onSelect: (TangemTabItem.Variant) -> Unit) {
    Section(label = "Variant") {
        ChipGrid(
            items = TangemTabItem.Variant.entries,
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
private fun ThemeSelector(selected: Theme, onSelect: (Theme) -> Unit) {
    Section(label = "Theme") {
        ChipGrid(
            items = Theme.entries,
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
private fun Toggles(state: TangemTabNavigationStory) {
    Section(label = "Flags") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ToggleRow(label = "counter", checked = state.hasCounter, onToggle = state.onCounterToggle)
            ToggleRow(label = "icon", checked = state.hasIcon, onToggle = state.onIconToggle)
            ToggleRow(label = "loading", checked = state.isLoading, onToggle = state.onLoadingToggle)
            ToggleRow(label = "blur", checked = state.isBlurEnabled, onToggle = state.onBlurToggle)
        }
    }
}

@Composable
private fun Hint() {
    Text(
        modifier = Modifier.padding(horizontal = 16.dp),
        text = "Tap a tab to select it. Hold one down to see the pressed state, " +
            "or connect a keyboard and use the arrow keys for the focus ring.",
        style = TangemTheme.typography.caption2,
        color = TangemTheme.colors.text.secondary,
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
    // The scroller is the outer box and the pill is the inner row, so the pill is measured against
    // unbounded width and extends past the screen instead of wrapping. Putting the scroll on the pill
    // itself would size it to the viewport, and its background and border would stop at the screen edge.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            // Inside the scroll, so it reads as content padding: the pill starts 16dp in, and the same
            // gap is reachable past its end.
            .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier
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
                )
            }
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
            .padding(vertical = 8.dp, horizontal = 12.dp),
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