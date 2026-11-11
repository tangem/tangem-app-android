package com.tangem.core.ui.ds2.button

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.haptic.HapticManager
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.core.ui.res.LocalHapticManager
import com.tangem.core.ui.res.TangemTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Hold-to-confirm wiring for [TangemButtonContainer].
 *
 * @property hint Label shown in place of the button text for a moment after an early release.
 * @property isActionEnabled Whether accessibility services may trigger [onConfirm] directly.
 */
@Immutable
internal class HoldToConfirmSpec(
    val state: HoldToConfirmState,
    val overlayColor: Color,
    val hint: TextReference,
    val isActionEnabled: Boolean,
    val onConfirm: () -> Unit,
)

/**
 * Animated values of one hold-to-confirm button, timed after the Figma `// Animation` frame
 * ("Hold and release" / "Hold and load").
 *
 * @param initialFill Starting fill, `0..1` — only previews start above `0`.
 */
@Stable
internal class HoldToConfirmState(initialFill: Float = 0f) {

    /** Overlay width as a fraction of the button, grown from the start edge while held. */
    val fill = Animatable(initialFill)

    /** Overlay opacity — faded out when a hold completes, so the loader takes over. */
    val fillAlpha = Animatable(1f)

    /** Opacity of the full-width press pulse played at touch-down. */
    val pulse = Animatable(0f)

    /** Horizontal offset of the button in dp, shaken after an early release. */
    val shakeOffsetDp = Animatable(0f)

    var isHintVisible by mutableStateOf(false)
        private set

    private var hintJob: Job? = null

    suspend fun playPulse() {
        pulse.animateTo(targetValue = 1f, animationSpec = tween(PULSE_HALF_MS, easing = StandardEasing))
        pulse.animateTo(targetValue = 0f, animationSpec = tween(PULSE_HALF_MS, easing = StandardEasing))
    }

    suspend fun fillToEnd() {
        fill.animateTo(targetValue = 1f, animationSpec = tween(HOLD_DURATION_MS, easing = LinearEasing))
    }

    suspend fun fadeOutCompletedFill() {
        fillAlpha.animateTo(targetValue = 0f, animationSpec = tween(COMPLETE_FADE_MS, easing = StandardEasing))
        fill.snapTo(0f)
        fillAlpha.snapTo(1f)
    }

    suspend fun retract() {
        fill.animateTo(targetValue = 0f, animationSpec = tween(RETRACT_MS, easing = EaseOut))
    }

    suspend fun shake() {
        shakeOffsetDp.animateTo(
            targetValue = 0f,
            animationSpec = keyframes {
                durationMillis = SHAKE_MS
                0f at 0 using EaseOut
                SHAKE_AMPLITUDE_DP at SHAKE_OUT_MS using EaseInOut
                SHAKE_LEFT_DP at SHAKE_MS - SHAKE_OUT_MS using EaseInOut
            },
        )
    }

    /** Shows the hint and schedules its hide; a later release restarts the countdown. */
    fun showHint(scope: CoroutineScope) {
        hintJob?.cancel()
        isHintVisible = true
        hintJob = scope.launch {
            delay(HINT_HIDE_DELAY_MS)
            isHintVisible = false
        }
    }

    fun hideHint() {
        hintJob?.cancel()
        isHintVisible = false
    }

    /**
     * Cancels a hold in progress. A completed hold is left alone: its caller typically switches the
     * button to loading in `onConfirm`, which disables holding, and the completion fade must still play.
     */
    suspend fun cancelHold() {
        hideHint()
        pulse.snapTo(0f)
        if (fill.isRunning) {
            fill.stop()
            fill.snapTo(0f)
        }
    }
}

/**
 * Press-and-hold gesture driving [state]: a press pulse at touch-down, then the fill runs `0 → 1`
 * while the pointer stays down, with haptic ticks that speed up as it fills. When it completes a
 * confirm haptic plays, [onConfirm] fires and the overlay fades out. Releasing early — or a parent
 * scroll stealing the pointer — retracts the fill, shakes the button and shows the hint.
 *
 * The fill runs in a scope outside the pointer handler, so turning [isEnabled] off mid-hold has to
 * stop it explicitly — otherwise a button that just became disabled or loading could still confirm.
 */
@Composable
internal fun rememberHoldToConfirmGesture(
    isEnabled: Boolean,
    state: HoldToConfirmState,
    onConfirm: () -> Unit,
): Modifier {
    val scope = rememberCoroutineScope()
    val hapticManager = LocalHapticManager.current
    val currentOnConfirm by rememberUpdatedState(onConfirm)

    LaunchedEffect(isEnabled) {
        if (!isEnabled) state.cancelHold()
    }

    return Modifier.pointerInput(isEnabled) {
        if (!isEnabled) return@pointerInput
        awaitEachGesture {
            awaitFirstDown()
            scope.launch { state.playPulse() }
            val fillJob = scope.launch {
                val ticks = launch { tickWhileFilling(hapticManager, state) }
                state.fillToEnd()
                ticks.cancel()
                hapticManager.perform(TangemHapticEffect.View.Confirm)
                state.hideHint()
                currentOnConfirm()
                state.fadeOutCompletedFill()
            }
            waitForUpOrCancellation()
            if (fillJob.isActive) {
                fillJob.cancel()
                hapticManager.perform(TangemHapticEffect.View.Reject)
                scope.launch { state.retract() }
                scope.launch { state.shake() }
                state.showHint(scope)
            }
        }
    }
}

/** Clock ticks whose interval shrinks quadratically with the fill — slow at first, a buzz near the end. */
private suspend fun tickWhileFilling(hapticManager: HapticManager, state: HoldToConfirmState) {
    while (true) {
        hapticManager.perform(TangemHapticEffect.View.ClockTick)
        val fill = state.fill.value
        delay((TICK_MAX_EXTRA_INTERVAL_MS * (1f - fill * fill)).toLong() + TICK_MIN_INTERVAL_MS)
    }
}

/**
 * Focusability, background, press pulse, hold overlay and shake of a hold-to-confirm button. The
 * background is painted here rather than by [com.tangem.core.ui.ds2.surface.TangemSurface] so the
 * overlays land between the fill and the content.
 */
internal fun Modifier.holdChrome(
    hold: HoldToConfirmSpec,
    backgroundColor: Color,
    isEnabled: Boolean,
    interactionSource: MutableInteractionSource,
): Modifier = focusable(enabled = isEnabled, interactionSource = interactionSource)
    .clip(CircleShape)
    .background(backgroundColor)
    .drawBehind {
        val pulse = hold.state.pulse.value
        if (pulse > 0f) drawRect(color = hold.overlayColor, alpha = pulse)
        val fill = hold.state.fill.value.coerceIn(0f, 1f)
        if (fill > 0f) {
            drawRect(
                color = hold.overlayColor,
                size = size.copy(width = size.width * fill),
                alpha = hold.state.fillAlpha.value,
            )
        }
    }

internal fun Modifier.holdShake(state: HoldToConfirmState): Modifier =
    graphicsLayer { translationX = state.shakeOffsetDp.value.dp.toPx() }

internal fun TangemButton.HoldVariant.toVariant(): TangemButton.Variant = when (this) {
    TangemButton.HoldVariant.Brand -> TangemButton.Variant.Brand
    TangemButton.HoldVariant.Primary -> TangemButton.Variant.Primary
}

@Composable
@ReadOnlyComposable
internal fun TangemButton.HoldVariant.holdOverlayColor(): Color = when (this) {
    TangemButton.HoldVariant.Brand -> TangemTheme.colors3.interaction.press.staticLight
    TangemButton.HoldVariant.Primary -> TangemTheme.colors3.interaction.press.inverse
}

@Composable
internal fun rememberHoldToConfirmState(): HoldToConfirmState = remember { HoldToConfirmState() }

/** Label ↔ hint crossfade: each side fades over this, the incoming one [HINT_FADE_IN_DELAY_MS] later. */
internal const val HINT_FADE_MS = 140
internal const val HINT_FADE_IN_DELAY_MS = 100

private const val HOLD_DURATION_MS = 2000
private const val PULSE_HALF_MS = 100
private const val COMPLETE_FADE_MS = 50
private const val RETRACT_MS = 240
private const val SHAKE_MS = 240
private const val SHAKE_OUT_MS = 60
private const val SHAKE_AMPLITUDE_DP = 8f
private const val SHAKE_LEFT_DP = -SHAKE_AMPLITUDE_DP

/** Release → hint fully in (240 ms) + 800 ms on screen, after which it crossfades back to the label. */
private const val HINT_HIDE_DELAY_MS = 1040L

private const val TICK_MIN_INTERVAL_MS = 15L
private const val TICK_MAX_EXTRA_INTERVAL_MS = 50f

private val StandardEasing = CubicBezierEasing(a = 0.5f, b = 0f, c = 0.5f, d = 1f)