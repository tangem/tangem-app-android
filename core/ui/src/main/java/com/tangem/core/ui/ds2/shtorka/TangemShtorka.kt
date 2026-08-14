package com.tangem.core.ui.ds2.shtorka

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.extensions.conditionalCompose
import com.tangem.core.ui.res.LocalHazeState
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.flow.drop

/**
 * Design-system v2 shtorka: a persistent, non-modal bottom sheet
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=8289-6388&m=dev)
 *
 * The material rendering blurs whatever is behind the card, so the screen content behind the
 * shtorka must be marked as a haze source (see `Modifier.hazeSourceTangem`).
 *
 * Inside the card, [LocalHazeState] is rescoped to the shtorka's own content: material components
 * in [header] (e.g. the round `TangemTopNavigation` buttons) blur the shtorka surface and the
 * [content] scrolling beneath them — not the screen behind the shtorka.
 *
 * Place the shtorka as the last child of a full-screen, edge-to-edge `Box`, over the main content.
 * At partial detents the card floats above [windowInsets]' bottom (the navigation bar); at
 * [TangemShtorka.Detent.Full] it extends beneath it, so scrollable [content] should add its own
 * navigation-bar content padding. Apply top window inset padding to [modifier] if the shtorka must
 * not reach the status bar at full.
 *

 * @param modifier the modifier for the shtorka's full-parent container (not the card)
 * @param isMaterial render the card as a translucent blurred material (default) or as a flat
 *   [color] surface. See [TangemSurface].
 * @param materialStyle material token set used when [isMaterial] is `true`. See
 *   [TangemSurface.MaterialStyle].
 * @param color card background used when [isMaterial] is `false`; in material mode it is the
 *   opaque surface the card fades into as it approaches [TangemShtorka.Detent.Full]
 * @param isSwipeEnabled when `false` the shtorka ignores drags and nested scroll; it can still be
 *   moved programmatically via [TangemShtorkaState.animateTo]
 * @param windowInsets system insets the card respects: at partial detents the floating card keeps
 *   clear of the bottom inset (navigation bar), and [TangemShtorka.Detent.Full] stops below the
 *   top inset (status bar). Pass [WindowInsets.Companion.systemBars] (default) for edge-to-edge
 *   screens or zero insets if the parent already applies padding
 * @param showDragHandle whether the grabber pill is shown at the top of the card
 * @param dragHandleContentDescription accessibility label announced by TalkBack for the grabber
 * @param header optional pinned overlay at the top of the card (below the grabber), typically a
 *   `TangemTopNavigation`; [content] scrolls beneath it and its material elements blur the shtorka
 * @param content content of the card, clipped to the card shape. It underlays the grabber and
 *   [header], so scrollable content should add matching top content padding
 */
@Composable
fun TangemShtorka(
    state: TangemShtorkaState,
    modifier: Modifier = Modifier,
    isMaterial: Boolean = true,
    materialStyle: TangemSurface.MaterialStyle = TangemSurface.MaterialStyle.Modal,
    color: Color = TangemTheme.colors3.bg.primary,
    isSwipeEnabled: Boolean = true,
    windowInsets: WindowInsets = WindowInsets.systemBars,
    showDragHandle: Boolean = true,
    dragHandleContentDescription: String? = null,
    header: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = remember(state) {
        ShtorkaShape(
            expandProgress = { state.expandProgress },
            collapseProgress = { state.collapseProgress },
        )
    }
    val nestedScrollConnection = remember(state) { state.shtorkaNestedScrollConnection() }

    val haptic = LocalHapticFeedback.current
    LaunchedEffect(state) {
        snapshotFlow { state.currentDetent }
            .drop(1)
            .collect { haptic.performHapticFeedback(HapticFeedbackType.SegmentTick) }
    }

    TangemSurface(
        modifier = modifier
            .shtorkaLayout(state = state, windowInsets = windowInsets)
            .graphicsLayer {
                translationY = rubberBandOffset(raw = state.rawOverdrag, limit = ShtorkaOverdragLimit.toPx())
            }
            .conditionalCompose(isSwipeEnabled) {
                nestedScroll(nestedScrollConnection)
                    .draggable(
                        state = state.draggableState,
                        orientation = Orientation.Vertical,
                        startDragImmediately = state.isSettling,
                        onDragStopped = { velocity -> state.startSettle(velocity) },
                    )
            },
        color = color,
        isMaterial = isMaterial,
        materialStyle = materialStyle,
        shape = shape,
    ) {
        // The card content is its own haze source, so glass elements of the header (nav buttons)
        // blur the shtorka surface and the content scrolling beneath them — not the screen behind.
        val outerHazeState = LocalHazeState.current
        val contentHazeState = rememberHazeState()
        SideEffect { contentHazeState.blurEnabled = outerHazeState.blurEnabled }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .conditionalCompose(isMaterial) {
                        // The glass fades into an opaque surface as the card approaches full.
                        drawBehind { drawRect(color = color.copy(alpha = state.expandProgress)) }
                    }
                    .hazeSourceTangem(state = contentHazeState),
            ) {
                content()
            }
            CompositionLocalProvider(LocalHazeState provides contentHazeState) {
                Column(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)) {
                    if (showDragHandle) {
                        DragHandle(contentDescription = dragHandleContentDescription)
                    }
                    header?.invoke(this)
                }
            }
        }
    }
}

/** Public namespace of the shtorka component. */
object TangemShtorka {

    /**
     * A position the shtorka can rest at, expressed as the visible height of the card. The card
     * additionally floats above its per-detent bottom margin (24dp when collapsed, 8dp at
     * intermediate detents, plus the system inset), so the resolved height is what the user
     * actually sees. Detents resolving to the same position collapse into one anchor.
     */
    @Immutable
    sealed interface Detent {

        /** Fixed visible card height, e.g. exactly a header. Coerced to the parent height at most. */
        data class Height(val value: Dp) : Detent

        /** Visible card height as a fraction (`0..1`) of the parent height. */
        data class Fraction(val value: Float) : Detent {
            init {
                require(value in 0f..1f) { "Fraction detent must be within 0..1" }
            }
        }

        /** The entire parent height; the card is edge-to-edge at this detent. */
        data object Full : Detent
    }
}

@Suppress("MagicNumber")
@Composable
private fun DragHandle(contentDescription: String?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .conditionalCompose(contentDescription != null) {
                semantics { this.contentDescription = requireNotNull(contentDescription) }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 4.dp)
                .background(color = TangemTheme.colors3.icon.tertiary, shape = CircleShape),
        )
    }
}

// region preview

@Preview(name = "Light", showBackground = true, heightDp = 640)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, heightDp = 640)
@Composable
private fun TangemShtorkaPreview() {
    TangemThemePreviewRedesign {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(previewMapBrush()),
        ) {
            TangemShtorka(
                state = rememberTangemShtorkaState(
                    detents = listOf(
                        TangemShtorka.Detent.Height(96.dp),
                        TangemShtorka.Detent.Fraction(0.45f),
                        TangemShtorka.Detent.Full,
                    ),
                    initialDetent = TangemShtorka.Detent.Fraction(0.45f),
                ),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Nearby",
                        color = TangemTheme.colors3.text.primary,
                        style = TangemTheme.typography3.heading.medium,
                    )
                    repeat(times = 4) { index ->
                        Text(
                            text = "Place ${index + 1}",
                            color = TangemTheme.colors3.text.secondary,
                            style = TangemTheme.typography3.body.medium,
                        )
                    }
                }
            }
        }
    }
}

@Suppress("MagicNumber")
@Composable
private fun previewMapBrush(): Brush {
    return Brush.verticalGradient(
        colors = listOf(
            TangemTheme.colors3.bg.accent.blue,
            TangemTheme.colors3.bg.secondary,
            TangemTheme.colors3.bg.tertiary,
        ),
    )
}

// endregion