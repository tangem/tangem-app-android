package com.tangem.core.ui.components.buttons.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationEndReason
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring.DampingRatioLowBouncy
import androidx.compose.animation.core.Spring.StiffnessLow
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.TextButton
import com.tangem.core.ui.haptic.HapticManager
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.core.ui.res.LocalHapticManager
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val HOLD_DURATION_MS = 1500L
private const val SCALE_DOWN_DURATION_MS = 1200
private const val SCALE_RESTORE_DURATION_MS = 300

private const val PRESSED_SCALE = 0.95f
private const val DEFAULT_SCALE = 1f

private const val SHAKE_AMPLITUDE_DP = 5f
private const val SHAKE_DURATION_MS = 120
private const val FADE_DURATION_MS = 150

private const val BOUNCE_VELOCITY_MULTIPLIER = 1.25f

private const val HAPTIC_INITIAL_INTERVAL_MS = 200L
private const val HAPTIC_MIN_INTERVAL_MS = 30L
private const val HAPTIC_HEARTBEAT_INTERVAL_MS = 100L

// Easing: smooth acceleration, gradually picking up speed
private val AccelerateEasing = CubicBezierEasing(
    a = 0.5f,
    b = 0f,
    c = 0.8f,
    d = 0.3f,
)

@Composable
internal fun TangemHoldToConfirmButton(
    text: String,
    hintText: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    colors: ButtonColors = TangemButtonsDefaults.primaryButtonColors,
    size: TangemButtonSize = TangemButtonSize.Default,
    textStyle: TextStyle = TangemTheme.typography.button,
    shape: Shape = size.toShape(),
) {
    val coroutineScope = rememberCoroutineScope()
    val state = rememberHoldToConfirmState()
    val hapticManager = LocalHapticManager.current

    // Reset state when loading transitions from true to false (e.g., after error) to allow retry
    var isPreviousLoading by remember { mutableStateOf(isLoading) }
    LaunchedEffect(isLoading) {
        if (isPreviousLoading && !isLoading && state.isConfirmed) {
            // Fade out loading
            state.textAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = FADE_DURATION_MS),
            )

            // Reset state
            state.reset()
            state.resetAnimations()

            // Fade in text
            state.textAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = FADE_DURATION_MS),
            )
        }
        isPreviousLoading = isLoading
    }

    val fillColor = TangemTheme.colors.icon.inactive.copy(alpha = 0.15f)
    val buttonHeight = size.toHeightDp()
    val contentPadding = size.toContentPadding(icon = TangemButtonIconPosition.None)

    val containerColor = if (enabled) colors.containerColor else colors.disabledContainerColor
    val contentColor = if (enabled) colors.contentColor else colors.disabledContentColor

    val shakeAmplitudePx = with(LocalDensity.current) { SHAKE_AMPLITUDE_DP.dp.toPx() }

    val shouldShowProgress = state.isProgressVisible || isLoading

    Surface(
        modifier = modifier
            .heightIn(min = buttonHeight)
            .graphicsLayer {
                scaleX = state.scaleProgress.value
                scaleY = state.scaleProgress.value
            }
            .holdToConfirmGestures(
                enabled = enabled,
                state = state,
                coroutineScope = coroutineScope,
                config = HoldToConfirmGestureConfig(
                    shakeAmplitudePx = shakeAmplitudePx,
                    hapticManager = hapticManager,
                    onConfirm = onConfirm,
                ),
            ),
        shape = shape,
        color = containerColor,
    ) {
        HoldToConfirmButtonContent(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = buttonHeight)
                .clip(shape),
            contentPadding = contentPadding,
            state = state,
            shouldShowProgress = shouldShowProgress,
            textConfig = HoldToConfirmTextConfig(
                text = text,
                hintText = hintText,
                style = textStyle,
            ),
            color = HoldToConfirmButtonColor(
                fillColor = fillColor,
                contentColor = contentColor,
            ),
        )
    }
}

@Stable
private class HoldToConfirmState {
    val fillProgress = Animatable(0f)
    val scaleProgress = Animatable(DEFAULT_SCALE)
    val shakeOffset = Animatable(0f)
    val textAlpha = Animatable(1f)

    var isProgressVisible by mutableStateOf(false)
    var isConfirmed by mutableStateOf(false)
    var isHintVisible by mutableStateOf(false)
    var hintTimerJob: Job? = null

    fun reset() {
        hintTimerJob?.cancel()
        hintTimerJob = null
        isProgressVisible = false
        isConfirmed = false
        isHintVisible = false
    }

    suspend fun resetAnimations() {
        fillProgress.snapTo(0f)
        scaleProgress.snapTo(DEFAULT_SCALE)
        shakeOffset.snapTo(0f)
    }
}

@Immutable
private class HoldToConfirmButtonColor(
    val fillColor: Color,
    val contentColor: Color,
)

@Immutable
private class HoldToConfirmTextConfig(
    val text: String,
    val hintText: String,
    val style: TextStyle,
)

@Immutable
private class HoldToConfirmGestureConfig(
    val shakeAmplitudePx: Float,
    val hapticManager: HapticManager,
    val onConfirm: () -> Unit,
)

private suspend fun Animatable<Float, *>.shake(amplitude: Float) {
    animateTo(
        targetValue = -amplitude,
        animationSpec = tween(durationMillis = SHAKE_DURATION_MS, easing = FastOutSlowInEasing),
    )
    animateTo(
        targetValue = amplitude,
        animationSpec = tween(durationMillis = SHAKE_DURATION_MS, easing = FastOutSlowInEasing),
    )
    animateTo(
        targetValue = 0f,
        animationSpec = tween(durationMillis = SHAKE_DURATION_MS, easing = FastOutSlowInEasing),
    )
}

@Composable
private fun rememberHoldToConfirmState(): HoldToConfirmState {
    val state = remember { HoldToConfirmState() }

    DisposableEffect(Unit) {
        onDispose { state.hintTimerJob?.cancel() }
    }

    return state
}

private fun Modifier.holdToConfirmGestures(
    enabled: Boolean,
    state: HoldToConfirmState,
    coroutineScope: CoroutineScope,
    config: HoldToConfirmGestureConfig,
): Modifier = pointerInput(enabled, state.isConfirmed) {
    if (!enabled || state.isConfirmed) return@pointerInput

    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)

        val scaleJob = coroutineScope.launch {
            state.scaleProgress.animateTo(
                targetValue = PRESSED_SCALE,
                animationSpec = tween(durationMillis = SCALE_DOWN_DURATION_MS, easing = LinearEasing),
            )
        }

        val hapticJob = coroutineScope.launch {
            performAcceleratingHapticFeedback(config.hapticManager, state)
        }

        val fillJob = coroutineScope.launch {
            val result = state.fillProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = HOLD_DURATION_MS.toInt(), easing = AccelerateEasing),
            )

            if (result.endReason == AnimationEndReason.Finished) {
                state.isConfirmed = true
                state.isProgressVisible = true

                // Soft heartbeat on success
                performSuccessHapticFeedback(config.hapticManager)

                state.scaleProgress.animateTo(
                    targetValue = DEFAULT_SCALE,
                    animationSpec = tween(durationMillis = SCALE_RESTORE_DURATION_MS, easing = FastOutSlowInEasing),
                )

                config.onConfirm()
            }
        }

        waitForUpOrCancellation()

        scaleJob.cancel()
        fillJob.cancel()
        hapticJob.cancel()

        coroutineScope.launch {
            handleReleaseAnimation(state, config)
        }
    }
}

/**
 * Performs accelerating haptic feedback during hold gesture.
 * Starts with slow ticks and accelerates as progress increases.
 */
private suspend fun performAcceleratingHapticFeedback(hapticManager: HapticManager, state: HoldToConfirmState) {
    val maxInterval = HAPTIC_INITIAL_INTERVAL_MS
    val minInterval = HAPTIC_MIN_INTERVAL_MS

    while (coroutineContext.isActive && !state.isConfirmed) {
        val progress = state.fillProgress.value
        // Exponential decrease: interval decreases faster as progress increases
        val interval = (maxInterval * (1f - progress * progress) + minInterval).toLong()

        hapticManager.perform(TangemHapticEffect.View.SegmentTick)
        delay(interval)
    }
}

/**
 * Performs strong heartbeat haptic feedback on release (two heavy clicks).
 */
private suspend fun performReleaseHapticFeedback(hapticManager: HapticManager) {
    hapticManager.perform(TangemHapticEffect.OneTime.HeavyClick)
    delay(HAPTIC_HEARTBEAT_INTERVAL_MS)
    hapticManager.perform(TangemHapticEffect.OneTime.HeavyClick)
}

/**
 * Performs soft heartbeat haptic feedback on success (two light clicks).
 */
private suspend fun performSuccessHapticFeedback(hapticManager: HapticManager) {
    hapticManager.perform(TangemHapticEffect.OneTime.Click)
    delay(HAPTIC_HEARTBEAT_INTERVAL_MS)
    hapticManager.perform(TangemHapticEffect.OneTime.Click)
}

private suspend fun CoroutineScope.handleReleaseAnimation(
    state: HoldToConfirmState,
    config: HoldToConfirmGestureConfig,
) {
    if (state.isConfirmed) return

    // Strong heartbeat on release (two heavy clicks)
    performReleaseHapticFeedback(config.hapticManager)

    state.fillProgress.snapTo(0f)

    val pressAmount = (DEFAULT_SCALE - state.scaleProgress.value) / (DEFAULT_SCALE - PRESSED_SCALE)
    val bounceVelocity = pressAmount * BOUNCE_VELOCITY_MULTIPLIER

    state.scaleProgress.animateTo(
        targetValue = DEFAULT_SCALE,
        animationSpec = spring(dampingRatio = DampingRatioLowBouncy, stiffness = StiffnessLow),
        initialVelocity = bounceVelocity,
    )

    if (state.isHintVisible) {
        state.hintTimerJob?.cancel()
        state.shakeOffset.shake(config.shakeAmplitudePx)
    } else {
        state.textAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = FADE_DURATION_MS),
        )

        state.isHintVisible = true

        launch {
            state.textAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = FADE_DURATION_MS),
            )
        }
        state.shakeOffset.shake(config.shakeAmplitudePx)
    }

    state.hintTimerJob = launch {
        delay(HOLD_DURATION_MS)

        state.textAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = FADE_DURATION_MS),
        )

        state.isHintVisible = false

        state.textAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = FADE_DURATION_MS),
        )
    }
}

@Composable
private fun HoldToConfirmButtonContent(
    state: HoldToConfirmState,
    shouldShowProgress: Boolean,
    contentPadding: PaddingValues,
    textConfig: HoldToConfirmTextConfig,
    color: HoldToConfirmButtonColor,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .drawBehind {
                if (state.fillProgress.value > 0f && !shouldShowProgress) {
                    drawRect(
                        color = color.fillColor,
                        topLeft = Offset.Zero,
                        size = Size(
                            width = this.size.width * state.fillProgress.value,
                            height = this.size.height,
                        ),
                    )
                }
            }
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        if (shouldShowProgress) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(TangemTheme.dimens.size24)
                    .graphicsLayer { alpha = state.textAlpha.value },
                color = color.contentColor,
                strokeWidth = TangemTheme.dimens.spacing2,
            )
        } else {
            Text(
                modifier = Modifier.graphicsLayer {
                    translationX = state.shakeOffset.value
                    alpha = state.textAlpha.value
                },
                text = if (state.isHintVisible) textConfig.hintText else textConfig.text,
                style = textConfig.style,
                color = color.contentColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// region Preview
@Preview(showBackground = true)
@Composable
private fun HoldToConfirmButtonPreview() {
    TangemThemePreview {
        var isLoading by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .background(TangemTheme.colors.background.primary)
                .padding(TangemTheme.dimens.spacing16),
        ) {
            TangemHoldToConfirmButton(
                modifier = Modifier.fillMaxWidth(),
                text = "Hold to Send",
                hintText = "Tap and hold",
                isLoading = isLoading,
                onConfirm = { isLoading = true },
            )

            Spacer(modifier = Modifier.height(TangemTheme.dimens.spacing16))

            TextButton(
                text = "Reset (simulate error)",
                onClick = { isLoading = false },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(TangemTheme.dimens.spacing8))

            Text(
                text = "isLoading: $isLoading",
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.secondary,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HoldToConfirmButtonDisabledPreview() {
    TangemThemePreview {
        TangemHoldToConfirmButton(
            text = "Hold to Send",
            hintText = "Tap and hold",
            onConfirm = {},
            enabled = false,
        )
    }
}
// endregion