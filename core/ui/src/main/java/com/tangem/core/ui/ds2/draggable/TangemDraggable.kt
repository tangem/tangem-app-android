package com.tangem.core.ui.ds2.draggable

import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tangem.core.ui.ds2.DsVersion
import com.tangem.core.ui.ds2.shape.SmoothCornerShape
import com.tangem.core.ui.extensions.conditional
import com.tangem.core.ui.extensions.softLayerShadow
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.core.ui.res.LocalHapticManager
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

/**
 * Design-system v2 draggable container: a rounded slot that lifts off the list — scales up, gains a
 * [TangemTheme.colors3] `bg.secondary` background and a drop shadow — while it is being dragged.
 * This overload only draws a given [state]; to reorder a `LazyColumn` use the `LazyItemScope`
 * overload with [rememberTangemDraggableState].
 *
 * Version: 1.0.0
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=11482-705)
 *
 * Behavior notes:
 * - [TangemDraggable.State.Default] has no background, so the content sits on whatever is behind it.
 * - Pressed shrinks to 98%, elevated grows to 102% and fades the background and shadow in; leaving
 *   elevated fades them out over the same 280 ms.
 * - Content is always clipped to the smoothed 20dp corners.
 *
 * @param state Visual state to draw, see [TangemDraggable.State].
 * @param modifier Modifier applied to the container; the container wraps its content.
 * @param content Slot content, e.g. a row.
 */
@Suppress("MagicNumber")
@Composable
fun TangemDraggable(state: TangemDraggable.State, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val transition = updateTransition(targetState = state, label = "TangemDraggable")
    val scale by transition.animateFloat(
        transitionSpec = {
            when {
                TangemDraggable.State.Elevated == initialState || TangemDraggable.State.Elevated == targetState ->
                    tween(durationMillis = 280, easing = EaseOut)
                // Delayed by the tap timeout: a scroll that starts on the item releases the press before
                // the shrink becomes visible.
                TangemDraggable.State.Pressed == targetState ->
                    tween(durationMillis = 120, delayMillis = 100, easing = EaseOut)
                else -> tween(durationMillis = 120, easing = EaseOut)
            }
        },
        label = "scale",
    ) { visualState ->
        when (visualState) {
            TangemDraggable.State.Default -> 1f
            TangemDraggable.State.Pressed -> 0.98f
            TangemDraggable.State.Elevated -> 1.02f
        }
    }
    val elevation by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 280, easing = EaseOut) },
        label = "elevation",
    ) { if (it == TangemDraggable.State.Elevated) 1f else 0f }

    val shadowColor = TangemTheme.colors3.interaction.press.staticLight
    val backgroundColor = TangemTheme.colors3.bg.secondary

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .conditional(elevation > 0f) {
                softLayerShadow(
                    radius = 40.dp,
                    color = shadowColor.copy(alpha = shadowColor.alpha * elevation),
                    shape = DraggableShape,
                    offset = DpOffset(x = 0.dp, y = 8.dp),
                )
            }
            .clip(DraggableShape)
            .background(backgroundColor.copy(alpha = backgroundColor.alpha * elevation)),
    ) {
        content()
    }
}

/**
 * Design-system v2 drag-to-reorder item of a vertical `LazyColumn`: [TangemDraggable] wired to a
 * [TangemDraggableState].
 *
 * Version: 1.0.0
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=11482-705)
 *
 * ```
 * val listState = rememberLazyListState()
 * val draggableState = rememberTangemDraggableState(listState, onMove = model::onMove, onDragEnd = model::onDragEnd)
 * LazyColumn(state = listState) {
 *     items(items, key = { it.id }) { item ->
 *         TangemDraggable(state = draggableState, key = item.id) { TangemRow(…) }
 *     }
 * }
 * ```
 *
 * Behavior notes:
 * - Pressing shrinks the item; a long press anywhere on it lifts it (haptic, elevated look) and it
 *   follows the finger. Releasing calls `onDragEnd` and springs it into its slot.
 * - Holding the finger near the top or bottom edge of the list scrolls it.
 * - A tap is left to the content, so a clickable row keeps working. A long press is not: the row's
 *   `onClick` doesn't fire after a drag, but an `onLongClick` of the content fires alongside it.
 * - Other items slide into place with `animateItem()`. Items that are not [TangemDraggable] (headers)
 *   need their own `Modifier.animateItem()`, or they jump when the dragged item passes them.
 *
 * @param state Shared state from [rememberTangemDraggableState].
 * @param key The key this item has in `items(key = …)`.
 * @param modifier Modifier applied to the container. Applies `animateItem()` itself — don't add it.
 * @param enabled `false` makes the item neither draggable nor a place other items can be dropped onto.
 * @param content Slot content, receiving whether this item is being dragged.
 */
@Composable
fun LazyItemScope.TangemDraggable(
    state: TangemDraggableState,
    key: Any,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable (isDragging: Boolean) -> Unit,
) {
    val hapticManager = LocalHapticManager.current
    var isPressed by remember { mutableStateOf(false) }
    val isDragging = state.draggingKey == key

    DisposableEffect(state, key, enabled) {
        if (enabled) state.dropTargets += key
        onDispose { state.dropTargets -= key }
    }

    TangemDraggable(
        state = when {
            isDragging -> TangemDraggable.State.Elevated
            isPressed -> TangemDraggable.State.Pressed
            else -> TangemDraggable.State.Default
        },
        // Pointer input sits outside the visual scale, or drag deltas would be scaled by it.
        modifier = modifier
            .then(
                if (state.isLifted(key)) {
                    Modifier
                        .zIndex(1f)
                        .graphicsLayer { translationY = state.translationOf(key) }
                } else {
                    Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null)
                },
            )
            .conditional(enabled) {
                pointerInput(state, key) {
                    detectTangemDrag(
                        state = state,
                        key = key,
                        onPressChange = { isPressed = it },
                        onDragStart = { hapticManager.perform(TangemHapticEffect.View.LongPress) },
                    )
                }
            },
    ) {
        content(isDragging)
    }
}

/** Public API surface of [TangemDraggable]. */
object TangemDraggable {

    val version = DsVersion(major = 1, minor = 0, patch = 0)

    /** Visual state of [TangemDraggable]. */
    enum class State {
        /** At rest: transparent, 100%. */
        Default,

        /** Held down: 98%. */
        Pressed,

        /** Lifted while dragged: 102%, `bg.secondary` background and drop shadow. */
        Elevated,
    }
}

private val DraggableShape = SmoothCornerShape(radius = 20.dp, smoothing = 0.6f)

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun TangemDraggablePreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            TangemDraggable.State.entries.forEach { state ->
                TangemDraggable(state = state, modifier = Modifier.fillMaxWidth()) {
                    PreviewRow(title = state.name)
                }
            }
        }
    }
}

@Composable
private fun PreviewRow(title: String) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PreviewTexts(top = title, bottom = "Subtitle", modifier = Modifier.weight(1f))
        PreviewTexts(top = "Value", bottom = "Subvalue", horizontalAlignment = Alignment.End)
    }
}

@Composable
private fun PreviewTexts(
    top: String,
    bottom: String,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
) {
    Column(modifier = modifier, horizontalAlignment = horizontalAlignment) {
        Text(text = top, color = TangemTheme.colors3.text.primary, style = TangemTheme.typography3.body.medium)
        Text(text = bottom, color = TangemTheme.colors3.text.secondary, style = TangemTheme.typography3.caption.medium)
    }
}