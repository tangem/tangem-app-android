package com.tangem.core.ui.ds2.topnavigation

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.ds2.button.Back
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.fade.TangemFade
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.rememberLastNonNull
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

private enum class SlotId { Start, Content, Group, End }

/**
 * Top navigation bar from the redesigned design system.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=2270-3&m=dev)
 *
 * @param contentAlign How [contentColumn] is aligned horizontally within the bar.
 * @param windowInsets Top inset applied above the row. Pass `WindowInsets(0)` inside a bottom
 *   sheet / modal.
 * @param contentPadding Inner padding applied to the row, inside [windowInsets]. Defaults to
 *   [TangemTopNavigation.DefaultContentPadding] (top 8, bottom 16, horizontal 16). Override a single
 *   edge via [com.tangem.core.ui.extensions.copy], e.g. `DefaultContentPadding.copy(top = 16.dp)`.
 * @param blurBackground Whether the fade behind the row should blur the content below.
 * @param startButton Leading slot. Typically a back button (see [TangemButton.Back]).
 * @param endButtonsGroup Optional pill-grouped secondary actions placed just before [endButton].
 * @param endButton Trailing slot. Typically a close button (see [TangemButton.Close]).
 * @param contentColumn Center slot. Place title/subtitle children here.
 */
@Suppress("LongMethod")
@Composable
fun TangemTopNavigation(
    modifier: Modifier = Modifier,
    contentAlign: TangemTopNavigation.ContentAlign = TangemTopNavigation.ContentAlign.Start,
    windowInsets: WindowInsets = WindowInsets.statusBars,
    contentPadding: PaddingValues = TangemTopNavigation.DefaultContentPadding,
    blurBackground: Boolean = true,
    startButton: (@Composable () -> Unit)? = null,
    endButtonsGroup: (@Composable RowScope.() -> Unit)? = null,
    endButton: (@Composable () -> Unit)? = null,
    contentColumn: (@Composable ColumnScope.() -> Unit)? = null,
) {
    // Shared, snappy specs so size and alpha animations stay in sync across all top-nav slots,
    // mirroring the convention used by TangemButtonInternal.
    val slotSizeSpec = remember { spring<IntSize>(stiffness = Spring.StiffnessMediumLow) }
    val slotAlphaSpec = remember { spring<Float>(stiffness = Spring.StiffnessMediumLow) }
    val slotEnter = remember(slotSizeSpec, slotAlphaSpec) {
        fadeIn(animationSpec = slotAlphaSpec) + expandHorizontally(animationSpec = slotSizeSpec)
    }
    val slotExit = remember(slotSizeSpec, slotAlphaSpec) {
        fadeOut(animationSpec = slotAlphaSpec) + shrinkHorizontally(animationSpec = slotSizeSpec)
    }

    Box(modifier) {
        TangemFade(
            modifier = Modifier.matchParentSize(),
            position = TangemFade.Position.Top,
            variant = TangemFade.Variant.Soft,
            blur = blurBackground,
        )

        val groupSpacing = 8.dp
        Layout(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(windowInsets)
                .padding(contentPadding),
            content = {
                val displayedStart = rememberLastNonNull(startButton)
                Box(modifier = Modifier.layoutId(SlotId.Start)) {
                    AnimatedVisibility(
                        visible = startButton != null,
                        enter = slotEnter,
                        exit = slotExit,
                    ) {
                        displayedStart?.invoke()
                    }
                }

                Column(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .layoutId(SlotId.Content),
                    horizontalAlignment = when (contentAlign) {
                        TangemTopNavigation.ContentAlign.Start -> Alignment.Start
                        TangemTopNavigation.ContentAlign.Center -> Alignment.CenterHorizontally
                    },
                ) {
                    contentColumn?.invoke(this)
                }

                val displayedGroup = rememberLastNonNull(endButtonsGroup)
                Box(modifier = Modifier.layoutId(SlotId.Group)) {
                    AnimatedVisibility(
                        visible = endButtonsGroup != null,
                        enter = slotEnter,
                        exit = slotExit,
                    ) {
                        displayedGroup?.let { group ->
                            TangemSurface(isMaterial = true, shape = CircleShape) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    content = group,
                                )
                            }
                        }
                    }
                }

                val displayedEnd = rememberLastNonNull(endButton)
                Box(modifier = Modifier.layoutId(SlotId.End)) {
                    AnimatedVisibility(
                        visible = endButton != null,
                        enter = slotEnter,
                        exit = slotExit,
                    ) {
                        displayedEnd?.invoke()
                    }
                }
            },
        ) { measurables, constraints ->
            val groupSpacingPx = groupSpacing.roundToPx()
            val totalWidth = constraints.maxWidth

            val startM = measurables.first { it.layoutId == SlotId.Start }
            val contentM = measurables.first { it.layoutId == SlotId.Content }
            val groupM = measurables.first { it.layoutId == SlotId.Group }
            val endM = measurables.first { it.layoutId == SlotId.End }

            val slotConstraints = constraints.copy(minWidth = 0)
            val startP = startM.measure(slotConstraints)
            val endP = endM.measure(slotConstraints)
            val groupP = groupM.measure(slotConstraints)

            val contentMaxWidth = when (contentAlign) {
                // Symmetric band so the content can be visually centered within `totalWidth`
                // without colliding with the start/end slots.
                TangemTopNavigation.ContentAlign.Center ->
                    (totalWidth - 2 * maxOf(startP.width, endP.width)).coerceAtLeast(0)
                TangemTopNavigation.ContentAlign.Start ->
                    (totalWidth - startP.width - endP.width).coerceAtLeast(0)
            }
            val contentP = contentM.measure(slotConstraints.copy(maxWidth = contentMaxWidth))

            val rowHeight = maxOf(startP.height, contentP.height, endP.height, groupP.height)

            layout(totalWidth, rowHeight) {
                fun centerY(h: Int) = (rowHeight - h) / 2
                startP.placeRelative(x = 0, y = centerY(startP.height))

                val contentX = when (contentAlign) {
                    TangemTopNavigation.ContentAlign.Start -> startP.width
                    TangemTopNavigation.ContentAlign.Center ->
                        ((totalWidth - contentP.width) / 2)
                            .coerceIn(
                                startP.width,
                                (totalWidth - endP.width - contentP.width).coerceAtLeast(startP.width),
                            )
                }
                contentP.placeRelative(x = contentX, y = centerY(contentP.height))

                endP.placeRelative(x = totalWidth - endP.width, y = centerY(endP.height))
                // Group floats to the left of endButton with `groupSpacing` gap, overlaying the
                // tail of the content band if necessary.
                val groupX = (totalWidth - endP.width - groupSpacingPx - groupP.width)
                    .coerceAtLeast(0)
                groupP.placeRelative(x = groupX, y = centerY(groupP.height))
            }
        }
    }
}

/** [TangemTopNavigation] with predefined back / close buttons and a title / subtitle center. */
@Composable
fun TangemTopNavigation(
    title: TextReference,
    modifier: Modifier = Modifier,
    subtitle: TextReference? = null,
    contentAlign: TangemTopNavigation.ContentAlign = TangemTopNavigation.ContentAlign.Start,
    windowInsets: WindowInsets = WindowInsets.statusBars,
    contentPadding: PaddingValues = TangemTopNavigation.DefaultContentPadding,
    blurBackground: Boolean = true,
    onBack: (() -> Unit)? = null,
    endButtonsGroup: (@Composable RowScope.() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
) {
    TangemTopNavigation(
        modifier = modifier,
        contentAlign = contentAlign,
        windowInsets = windowInsets,
        contentPadding = contentPadding,
        blurBackground = blurBackground,
        startButton = onBack?.let { { TangemButton.Back(onClick = it) } },
        endButtonsGroup = endButtonsGroup,
        endButton = onClose?.let { { TangemButton.Close(onClick = it) } },
        contentColumn = { TitleSubtitle(title = title, subtitle = subtitle) },
    )
}

/** [TangemTopNavigation] with a custom [startButton], title / subtitle center, and predefined close. */
@Composable
fun TangemTopNavigation(
    title: TextReference,
    modifier: Modifier = Modifier,
    subtitle: TextReference? = null,
    contentAlign: TangemTopNavigation.ContentAlign = TangemTopNavigation.ContentAlign.Start,
    windowInsets: WindowInsets = WindowInsets.statusBars,
    contentPadding: PaddingValues = TangemTopNavigation.DefaultContentPadding,
    blurBackground: Boolean = true,
    endButtonsGroup: (@Composable RowScope.() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    startButton: @Composable () -> Unit,
) {
    TangemTopNavigation(
        modifier = modifier,
        contentAlign = contentAlign,
        windowInsets = windowInsets,
        contentPadding = contentPadding,
        blurBackground = blurBackground,
        startButton = startButton,
        endButtonsGroup = endButtonsGroup,
        endButton = onClose?.let { { TangemButton.Close(onClick = it) } },
        contentColumn = { TitleSubtitle(title = title, subtitle = subtitle) },
    )
}

/** [TangemTopNavigation] with predefined back, title / subtitle center, and a custom [endButton]. */
@Composable
fun TangemTopNavigation(
    title: TextReference,
    modifier: Modifier = Modifier,
    subtitle: TextReference? = null,
    contentAlign: TangemTopNavigation.ContentAlign = TangemTopNavigation.ContentAlign.Start,
    windowInsets: WindowInsets = WindowInsets.statusBars,
    contentPadding: PaddingValues = TangemTopNavigation.DefaultContentPadding,
    blurBackground: Boolean = true,
    onBack: (() -> Unit)? = null,
    endButton: @Composable () -> Unit,
) {
    TangemTopNavigation(
        modifier = modifier,
        contentAlign = contentAlign,
        windowInsets = windowInsets,
        contentPadding = contentPadding,
        blurBackground = blurBackground,
        startButton = onBack?.let { { TangemButton.Back(onClick = it) } },
        endButton = endButton,
        contentColumn = { TitleSubtitle(title = title, subtitle = subtitle) },
    )
}

@Composable
private fun ColumnScope.TitleSubtitle(title: TextReference, subtitle: TextReference?) {
    val sizeSpec = remember { spring<IntSize>(stiffness = Spring.StiffnessMediumLow) }
    val alphaSpec = remember { spring<Float>(stiffness = Spring.StiffnessMediumLow) }

    // Title swaps in place (no size change), so a pure cross-fade reads better than expand/shrink.
    val titleEnter = remember(alphaSpec) { fadeIn(animationSpec = alphaSpec) }
    val titleExit = remember(alphaSpec) { fadeOut(animationSpec = alphaSpec) }

    // Subtitle pushes the bar down/up, so animate height instead of width.
    val subtitleEnter = remember(sizeSpec, alphaSpec) {
        fadeIn(animationSpec = alphaSpec) + expandVertically(animationSpec = sizeSpec)
    }
    val subtitleExit = remember(sizeSpec, alphaSpec) {
        fadeOut(animationSpec = alphaSpec) + shrinkVertically(animationSpec = sizeSpec)
    }

    // Title swaps via cross-fade whenever the reference changes (e.g. step-driven flows).
    AnimatedContent(
        targetState = title,
        transitionSpec = { titleEnter togetherWith titleExit },
        label = "TangemTopNavigation.title",
    ) { current ->
        TangemNavigationText(text = current, role = TangemNavigationText.Role.Title)
    }

    // Subtitle expands/shrinks vertically so the title doesn't visually jump when toggled.
    val displayedSubtitle = rememberLastNonNull(subtitle)
    AnimatedVisibility(
        visible = subtitle != null,
        enter = subtitleEnter,
        exit = subtitleExit,
    ) {
        displayedSubtitle?.let { text ->
            Column {
                Spacer(Modifier.height(4.dp))
                TangemNavigationText(text = text, role = TangemNavigationText.Role.Subtitle)
            }
        }
    }
}

object TangemTopNavigation {

    /** Default inner padding of the row: top 8, bottom 16, horizontal 16. */
    @Suppress("MagicNumber")
    val DefaultContentPadding: PaddingValues = PaddingValues(
        top = 8.dp,
        bottom = 16.dp,
        start = 16.dp,
        end = 16.dp,
    )

    /** Horizontal alignment of the center content slot. */
    enum class ContentAlign {
        Start, Center
    }
}

@Preview(
    showBackground = true,
    device = Devices.PIXEL_7_PRO,
)
@Preview(
    showBackground = true,
    device = Devices.PIXEL_7_PRO,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun Preview() {
    TangemThemePreviewRedesign {
        Column(
            Modifier
                .fillMaxWidth()
                .hazeSourceTangem()
                .background(TangemTheme.colors.background.secondary),
        ) {
            Spacer(Modifier.height(32.dp))
            // Screen-level usage: default insets reserve space for the system status bar.
            TangemTopNavigation(
                startButton = { TangemButton.Back { } },
                contentColumn = {
                    TangemNavigationText(text = "Title", role = TangemNavigationText.Role.Title)
                    Spacer(Modifier.height(4.dp))
                    TangemNavigationText(text = "Subtitle", role = TangemNavigationText.Role.Subtitle)
                },
                endButtonsGroup = {
                    TangemButton(variant = TangemButton.Variant.Ghost) { }
                    TangemButton(variant = TangemButton.Variant.Ghost) { }
                },
                endButton = { TangemButton.Close { } },
            )
            Spacer(Modifier.height(16.dp))
            // Sheet/modal usage via the predefined-buttons overload: zero top inset.
            TangemTopNavigation(
                title = stringReference("Title"),
                subtitle = stringReference("Subtitle"),
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                windowInsets = WindowInsets(0),
                onClose = { },
            )
        }
    }
}