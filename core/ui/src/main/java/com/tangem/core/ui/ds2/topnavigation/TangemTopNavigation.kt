package com.tangem.core.ui.ds2.topnavigation

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.animation.TangemAnimationSpec
import com.tangem.core.ui.ds2.animation.TangemTransition
import com.tangem.core.ui.ds2.animation.WithoutMaterialWhileAnimating
import com.tangem.core.ui.ds2.button.Back
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.GroupEntry
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.button.TangemButtonStack
import com.tangem.core.ui.ds2.fade.TangemFade
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.rememberLastNonNull
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import kotlin.math.roundToInt

private enum class SlotId { Start, Content, Group, End }

@Composable
private fun EndSlot(endButton: (@Composable () -> Unit)?) {
    val displayedEnd = rememberLastNonNull(endButton)
    val reveal by animateFloatAsState(
        targetValue = if (endButton != null) 1f else 0f,
        animationSpec = TangemAnimationSpec.Reveal,
        label = "TangemTopNavigation.endReveal",
    )

    Box(modifier = Modifier.layoutId(SlotId.End)) {
        if (reveal > 0f) {
            Box(modifier = Modifier.revealFromBehindGroup { reveal }) {
                displayedEnd?.invoke()
            }
        }
    }
}

// A centered title needs the same gap on both sides, otherwise the column (and the text in it) is off-center.
private fun contentStartPadding(hasStartButton: Boolean, contentAlign: TangemTopNavigation.ContentAlign): Dp {
    return if (hasStartButton || contentAlign == TangemTopNavigation.ContentAlign.Center) 12.dp else 0.dp
}

private fun Modifier.revealFromBehindGroup(progress: () -> Float): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val width = (placeable.width * progress().coerceIn(0f, 1f)).roundToInt()

    layout(width = width, height = placeable.height) {
        placeable.place(x = width - placeable.width, y = 0)
    }
}

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
 * @param blurBackground Whether the fade behind the row should blur the content below. Place the bar
 *   as a sibling of the haze-source content, not inside it — nested in the source, the bar's material
 *   buttons have no area left to sample.
 * @param fadeEnabled Whether the fade behind the row is shown. Alpha is animated on change. Defaults to true.
 * @param startButton Leading slot. Typically a back button (see [TangemButton.Back]).
 * @param endButtonsGroup Optional secondary actions placed just before [endButton], grouped in a
 *   [TangemButtonStack]. Expected to emit its entries — see [TangemButton.GroupEntry].
 * @param isEndButtonsGroupBackgroundShown Whether the grouped secondary actions keep the stack's
 *   material pill ([TangemButtonStack.Variant.Material]) or drop it
 *   ([TangemButtonStack.Variant.Ghost]). Toggling fades the pill in place. Defaults to true.
 * @param endButton Trailing slot. Typically, a close button (see [TangemButton.Close]).
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
    fadeEnabled: Boolean = true,
    isEndButtonsGroupBackgroundShown: Boolean = true,
    startButton: (@Composable () -> Unit)? = null,
    endButtonsGroup: (@Composable RowScope.() -> Unit)? = null,
    endButton: (@Composable () -> Unit)? = null,
    contentColumn: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val fadeAlpha by animateFloatAsState(
        targetValue = if (fadeEnabled) 1f else 0f,
        animationSpec = TangemAnimationSpec.Alpha,
        label = "TangemTopNavigation.fadeAlpha",
    )

    Box(modifier) {
        // Own haze source, above the screen content's: without it the material buttons sample only
        // the content and punch a hole through the fade they sit on.
        Box(
            modifier = Modifier
                .matchParentSize()
                .hazeSourceTangem(zIndex = 1f),
        ) {
            TangemFade(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = fadeAlpha },
                position = TangemFade.Position.Top,
                variant = TangemFade.Variant.Soft,
                blur = blurBackground,
            )
        }

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
                        enter = TangemTransition.SlotEnterHorizontally,
                        exit = TangemTransition.SlotExitHorizontally,
                    ) {
                        WithoutMaterialWhileAnimating {
                            displayedStart?.invoke()
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .padding(
                            start = contentStartPadding(
                                hasStartButton = startButton != null,
                                contentAlign = contentAlign,
                            ),
                            end = 12.dp,
                        )
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
                        enter = TangemTransition.SlotEnterHorizontally,
                        exit = TangemTransition.SlotExitHorizontally,
                    ) {
                        WithoutMaterialWhileAnimating {
                            displayedGroup?.let { group ->
                                TangemButtonStack(
                                    variant = if (isEndButtonsGroupBackgroundShown) {
                                        TangemButtonStack.Variant.Material
                                    } else {
                                        TangemButtonStack.Variant.Ghost
                                    },
                                    content = group,
                                )
                            }
                        }
                    }
                }

                EndSlot(endButton = endButton)
            },
        ) { measurables, constraints ->
            val groupSpacingPx = 8.dp.roundToPx()
            val totalWidth = constraints.maxWidth

            val startM = measurables.first { it.layoutId == SlotId.Start }
            val contentM = measurables.first { it.layoutId == SlotId.Content }
            val groupM = measurables.first { it.layoutId == SlotId.Group }
            val endM = measurables.first { it.layoutId == SlotId.End }

            val slotConstraints = constraints.copy(minWidth = 0)
            val startP = startM.measure(slotConstraints)
            val endP = endM.measure(slotConstraints)
            val groupP = groupM.measure(slotConstraints)

            val endGap = if (endP.width > 0) groupSpacingPx else 0
            val groupOccupiedWidth = if (groupP.width > 0) endGap + groupP.width else 0
            val trailingWidth = endP.width + groupOccupiedWidth

            val contentMaxWidth = when (contentAlign) {
                TangemTopNavigation.ContentAlign.Center ->
                    (totalWidth - 2 * maxOf(startP.width, trailingWidth)).coerceAtLeast(0)
                TangemTopNavigation.ContentAlign.Start ->
                    (totalWidth - startP.width - trailingWidth).coerceAtLeast(0)
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
                                (totalWidth - trailingWidth - contentP.width).coerceAtLeast(startP.width),
                            )
                }
                contentP.placeRelative(x = contentX, y = centerY(contentP.height))

                endP.placeRelative(x = totalWidth - endP.width, y = centerY(endP.height))

                val groupX = (totalWidth - endP.width - endGap - groupP.width)
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
    fadeEnabled: Boolean = true,
    isEndButtonsGroupBackgroundShown: Boolean = true,
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
        fadeEnabled = fadeEnabled,
        isEndButtonsGroupBackgroundShown = isEndButtonsGroupBackgroundShown,
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
    fadeEnabled: Boolean = true,
    isEndButtonsGroupBackgroundShown: Boolean = true,
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
        fadeEnabled = fadeEnabled,
        isEndButtonsGroupBackgroundShown = isEndButtonsGroupBackgroundShown,
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
    fadeEnabled: Boolean = true,
    onBack: (() -> Unit)? = null,
    endButton: @Composable () -> Unit,
) {
    TangemTopNavigation(
        modifier = modifier,
        contentAlign = contentAlign,
        windowInsets = windowInsets,
        contentPadding = contentPadding,
        blurBackground = blurBackground,
        fadeEnabled = fadeEnabled,
        startButton = onBack?.let { { TangemButton.Back(onClick = it) } },
        endButton = endButton,
        contentColumn = { TitleSubtitle(title = title, subtitle = subtitle) },
    )
}

@Composable
private fun ColumnScope.TitleSubtitle(title: TextReference, subtitle: TextReference?) {
    // Title swaps in place (no size change), so a pure cross-fade reads better than expand/shrink,
    // while the subtitle pushes the bar down/up and therefore animates its height.
    AnimatedContent(
        targetState = title,
        transitionSpec = { TangemTransition.FadeEnter togetherWith TangemTransition.FadeExit },
        label = "TangemTopNavigation.title",
    ) { current ->
        TangemNavigationText(text = current, role = TangemNavigationText.Role.Title)
    }

    // Subtitle expands/shrinks vertically so the title doesn't visually jump when toggled.
    val displayedSubtitle = rememberLastNonNull(subtitle)
    AnimatedVisibility(
        visible = subtitle != null,
        enter = TangemTransition.SlotEnterVertically,
        exit = TangemTransition.SlotExitVertically,
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

    /**
     * Footprint of a navigation button ([TangemButton.Back] / [TangemButton.Close], both
     * [TangemButton.Size.X11]).
     *
     * The row is only as tall as its tallest slot, so a bar whose buttons come and go changes height and
     * shifts its centered title. Size a placeholder box with this value to hold an empty slot open, or to
     * align a non-button element (an icon, a brand mark) with the real buttons.
     */
    @Suppress("MagicNumber")
    val ButtonSlotSize: Dp = 44.dp

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
                .background(TangemTheme.colors3.bg.secondary),
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
                    val icon = TangemIconUM.Icon(iconRes = R.drawable.ic_information_24)
                    TangemButton.GroupEntry(iconUM = icon) { }
                    TangemButton.GroupEntry(iconUM = icon) { }
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