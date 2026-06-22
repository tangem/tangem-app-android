@file:Suppress("MagicNumber")

package com.tangem.feature.tester.presentation.storybook.page.ds.topnavigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.Back
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.topnavigation.TangemNavigationText
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.rememberLastNonNull
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.LocalHazeState
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_swap_horizontal_20
import com.tangem.core.ui.res.generated.icons.ic_scan_20
import com.tangem.core.ui.res.generated.icons.ic_sign_usd_20
import com.tangem.feature.tester.presentation.storybook.entity.TangemTopNavigationStory
import com.tangem.feature.tester.presentation.storybook.entity.TangemTopNavigationStory.Background
import com.tangem.feature.tester.presentation.storybook.entity.TangemTopNavigationStory.ContentMode
import com.tangem.feature.tester.presentation.storybook.entity.TangemTopNavigationStory.EndButton
import com.tangem.feature.tester.presentation.storybook.entity.TangemTopNavigationStory.EndGroup

private const val SHORT_TITLE = "Title"
private const val LONG_TITLE = "Very long top navigation title that should ellipsize"
private const val SUBTITLE = "Subtitle"
private const val PREVIEW_HEIGHT_DP = 220

// Match the production TangemTopNavigation's spring stiffness so the story animates identically.
private val SlotAlphaSpec = spring<Float>(stiffness = Spring.StiffnessMediumLow)
private val SlotSizeSpec = spring<androidx.compose.ui.unit.IntSize>(stiffness = Spring.StiffnessMediumLow)
private val TitleEnterTransition = fadeIn(animationSpec = SlotAlphaSpec)
private val TitleExitTransition = fadeOut(animationSpec = SlotAlphaSpec)
private val SubtitleEnterTransition =
    fadeIn(animationSpec = SlotAlphaSpec) + expandVertically(animationSpec = SlotSizeSpec)
private val SubtitleExitTransition =
    fadeOut(animationSpec = SlotAlphaSpec) + shrinkVertically(animationSpec = SlotSizeSpec)

@Composable
internal fun TangemTopNavigationStory(state: TangemTopNavigationStory, modifier: Modifier = Modifier) {
    val hazeState = LocalHazeState.current
    DisposableEffect(state.isBlurEnabled) {
        val wasBlurEnabled = hazeState.blurEnabled
        hazeState.blurEnabled = state.isBlurEnabled
        onDispose { hazeState.blurEnabled = wasBlurEnabled }
    }

    // Make the system status bar transparent for the lifetime of this story so the preview backdrop
    // visually merges with the system clock/icons area — same as a real edge-to-edge screen.
    // TesterActivity reapplies its solid bar color on its next recomposition (on exit).
    val systemUiController = rememberSystemUiController()
    DisposableEffect(systemUiController) {
        systemUiController.setStatusBarColor(color = Color.Transparent, darkIcons = false)
        onDispose { /* TesterActivity reapplies its color on next recomposition */ }
    }

    // TesterActivity wraps its NavHost in `Modifier.systemBarsPadding()`, which pushes this story
    // down below the status bar. To still let the preview cover the status bar, we shift it up by
    // the real status bar height and grow its height by the same amount.
    val statusBarDp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors.background.primary),
    ) {
        ComponentPreview(
            state = state,
            statusBarDp = statusBarDp,
            modifier = Modifier.align(Alignment.TopCenter),
        )
        Column(
            modifier = Modifier
                .padding(top = PREVIEW_HEIGHT_DP.dp + 16.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ContentAlignSelector(selected = state.contentAlign, onSelect = state.onContentAlignChange)
            ContentModeSelector(selected = state.contentMode, onSelect = state.onContentModeChange)
            EndButtonSelector(selected = state.endButton, onSelect = state.onEndButtonChange)
            EndGroupSelector(selected = state.endGroup, onSelect = state.onEndGroupChange)
            BackgroundSelector(selected = state.background, onSelect = state.onBackgroundChange)
            Toggles(state = state)
        }
    }
}

@Composable
private fun ComponentPreview(state: TangemTopNavigationStory, statusBarDp: Dp, modifier: Modifier = Modifier) {
    // The preview Box is grown by `statusBarDp` and shifted up by the same amount so its backdrop
    // bleeds into (now transparent) system status bar area. The top navigation itself uses a
    // matching `WindowInsets(top = statusBarDp)` so its content clears the system clock/icons,
    // exactly like in a real edge-to-edge screen.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(PREVIEW_HEIGHT_DP.dp + statusBarDp)
            .offset(y = -statusBarDp),
    ) {
        PreviewBackground(
            background = state.background,
            modifier = Modifier
                .fillMaxSize()
                .hazeSourceTangem(zIndex = 0f),
        )
        TangemTopNavigation(
            // The bar's outer Box (and its 96dp top fade) needs to reach the very top of the
            // preview, while only the inner row sits below the status bar. So we DON'T add outer
            // padding — instead we push the row down via `windowInsets`.
            //
            // Catch: TesterActivity wraps the NavHost in `Modifier.systemBarsPadding()`, which
            // marks the system bars insets as consumed for descendants. The bar's internal
            // `windowInsetsPadding(windowInsets)` subtracts that consumption (`statusBarDp`)
            // from whatever we pass. To still get an effective `statusBarDp` of row padding, we
            // pre-compensate by passing `2 * statusBarDp` here.
            modifier = Modifier.align(Alignment.TopCenter),
            contentAlign = state.contentAlign,
            windowInsets = if (state.useStatusBarInsets) WindowInsets(top = statusBarDp * 2) else WindowInsets(0),
            fadeBackground = state.isFadeEnabled,
            startButton = if (state.hasBack) {
                { TangemButton.Back(onClick = {}) }
            } else {
                null
            },
            endButtonsGroup = endGroupContent(state.endGroup),
            endButton = endButtonContent(state.endButton),
            contentColumn = {
                when (state.contentMode) {
                    ContentMode.Plain -> PlainContent(state)
                    ContentMode.Rich -> RichContent()
                }
            },
        )
    }
}

@Composable
private fun ColumnScope.PlainContent(state: TangemTopNavigationStory) {
    // Mirror the production `TitleSubtitle` helper so toggling the subtitle / swapping the title
    // animates the same way users see in real screens. The raw `contentColumn` slot doesn't wrap
    // children in any animation by itself.
    val title = if (state.longTitle) LONG_TITLE else SHORT_TITLE
    AnimatedContent(
        targetState = title,
        transitionSpec = { TitleEnterTransition togetherWith TitleExitTransition },
        label = "TangemTopNavigationStory.title",
    ) { current ->
        TangemNavigationText(text = current, role = TangemNavigationText.Role.Title)
    }
    val subtitle: String? = if (state.hasSubtitle) SUBTITLE else null
    val displayedSubtitle = rememberLastNonNull(subtitle)
    AnimatedVisibility(
        visible = subtitle != null,
        enter = SubtitleEnterTransition,
        exit = SubtitleExitTransition,
    ) {
        displayedSubtitle?.let { text ->
            Column {
                Spacer(Modifier.height(2.dp))
                TangemNavigationText(text = text, role = TangemNavigationText.Role.Subtitle)
            }
        }
    }
}

/**
 * Rich content variant — proves the `contentColumn` slot accepts an arbitrary composable, not just
 * a plain string. Uses [AnnotatedString] with per-span colors and an inline swap emoji to mimic
 * the multi-color "Title in 🔄 Title?" example from the design board.
 */
@Composable
private fun RichContent() {
    val titleSpan = buildAnnotatedString {
        append("Title in \uD83D\uDD04 ")
        withStyle(SpanStyle(color = Color(0xFFFF4D4F))) { append("Title?") }
    }
    val subtitleSpan = buildAnnotatedString {
        withStyle(SpanStyle(color = TangemTheme.colors3.text.secondary)) { append("Subtitle in ") }
        withStyle(SpanStyle(color = Color(0xFF34C759))) { append("Subtitle?") }
    }
    TangemNavigationText(text = titleSpan, role = TangemNavigationText.Role.Title)
    Spacer(Modifier.height(2.dp))
    TangemNavigationText(text = subtitleSpan, role = TangemNavigationText.Role.Subtitle)
}

private fun endButtonContent(endButton: EndButton): (@Composable () -> Unit)? = when (endButton) {
    EndButton.None -> null
    EndButton.Close -> {
        { TangemButton.Close(onClick = {}) }
    }
    EndButton.Loader -> {
        { TangemButton(variant = TangemButton.Variant.Material, isLoading = true, onClick = {}) }
    }
    EndButton.HowItWorks -> {
        {
            TangemButton(
                variant = TangemButton.Variant.Material,
                text = stringReference("How it works?"),
                onClick = {},
            )
        }
    }
}

private fun endGroupContent(endGroup: EndGroup): (@Composable RowScope.() -> Unit)? = when (endGroup) {
    EndGroup.None -> null
    EndGroup.One -> {
        {
            TangemButton(
                variant = TangemButton.Variant.Ghost,
                iconStart = TangemIconUM.Icon(Icons.ic_arrow_swap_horizontal_20),
                onClick = {},
            )
        }
    }
    EndGroup.Two -> {
        {
            TangemButton(
                variant = TangemButton.Variant.Ghost,
                iconStart = TangemIconUM.Icon(Icons.ic_arrow_swap_horizontal_20),
                onClick = {},
            )
            TangemButton(
                variant = TangemButton.Variant.Ghost,
                iconStart = TangemIconUM.Icon(Icons.ic_scan_20),
                onClick = {},
            )
        }
    }
    EndGroup.Three -> {
        {
            TangemButton(
                variant = TangemButton.Variant.Ghost,
                iconStart = TangemIconUM.Icon(Icons.ic_arrow_swap_horizontal_20),
                onClick = {},
            )
            TangemButton(
                variant = TangemButton.Variant.Ghost,
                iconStart = TangemIconUM.Icon(Icons.ic_sign_usd_20),
                onClick = {},
            )
            TangemButton(
                variant = TangemButton.Variant.Ghost,
                iconStart = TangemIconUM.Icon(Icons.ic_scan_20),
                onClick = {},
            )
        }
    }
}

@Composable
private fun PreviewBackground(background: Background, modifier: Modifier = Modifier) {
    when (background) {
        Background.Rainbow -> RainbowBackdrop(modifier)
        Background.BgPrimary -> Box(modifier.background(TangemTheme.colors3.bg.primary))
        Background.BgSecondary -> Box(modifier.background(TangemTheme.colors3.bg.secondary))
        Background.BgBrand -> Box(modifier.background(TangemTheme.colors3.bg.brand))
        Background.BgInverse -> Box(modifier.background(TangemTheme.colors3.bg.inverse))
    }
}

@Composable
private fun RainbowBackdrop(modifier: Modifier = Modifier) {
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
private fun ContentAlignSelector(
    selected: TangemTopNavigation.ContentAlign,
    onSelect: (TangemTopNavigation.ContentAlign) -> Unit,
) {
    Section(label = "Content align") {
        ChipGrid(
            items = TangemTopNavigation.ContentAlign.entries,
            label = { it.name },
            isSelected = { it == selected },
            onSelect = onSelect,
        )
    }
}

@Composable
private fun ContentModeSelector(selected: ContentMode, onSelect: (ContentMode) -> Unit) {
    Section(label = "Content mode") {
        ChipGrid(
            items = ContentMode.entries,
            label = { it.label },
            isSelected = { it == selected },
            onSelect = onSelect,
        )
    }
}

@Composable
private fun EndButtonSelector(selected: EndButton, onSelect: (EndButton) -> Unit) {
    Section(label = "End button") {
        ChipGrid(
            items = EndButton.entries,
            label = { it.label },
            isSelected = { it == selected },
            onSelect = onSelect,
        )
    }
}

@Composable
private fun EndGroupSelector(selected: EndGroup, onSelect: (EndGroup) -> Unit) {
    Section(label = "End buttons group (pill)") {
        ChipGrid(
            items = EndGroup.entries,
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
private fun Toggles(state: TangemTopNavigationStory) {
    Section(label = "Flags") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ToggleRow(label = "back button", checked = state.hasBack, onToggle = state.onBackToggle)
            ToggleRow(label = "subtitle", checked = state.hasSubtitle, onToggle = state.onSubtitleToggle)
            ToggleRow(
                label = "long title (ellipsis test)",
                checked = state.longTitle,
                onToggle = state.onLongTitleToggle,
            )
            ToggleRow(
                label = "status bar insets",
                checked = state.useStatusBarInsets,
                onToggle = state.onStatusBarInsetsToggle,
            )
            ToggleRow(label = "fade background", checked = state.isFadeEnabled, onToggle = state.onFadeToggle)
            ToggleRow(label = "blur (haze, needs fade)", checked = state.isBlurEnabled, onToggle = state.onBlurToggle)
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