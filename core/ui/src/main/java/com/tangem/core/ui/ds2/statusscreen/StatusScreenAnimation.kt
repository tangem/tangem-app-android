package com.tangem.core.ui.ds2.statusscreen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

/**
 * A linear clock over the status-screen entrance timeline, in milliseconds. Elements derive their own
 * motion from it via [segment], so every track stays on the single choreography defined in the DS figma.
 */
internal fun interface EntranceProgress {
    fun timeMs(): Float
}

/** Eased 0..1 fraction of the [startMs]..[endMs] window at the clock's current time. */
internal fun EntranceProgress.segment(startMs: Int, endMs: Int, easing: Easing): Float {
    val fraction = ((timeMs() - startMs) / (endMs - startMs)).coerceIn(0f, 1f)
    return easing.transform(fraction).coerceIn(0f, 1f)
}

@Composable
internal fun rememberEntranceProgress(durationMs: Int, animated: Boolean): EntranceProgress {
    val timeMs = remember(animated) { Animatable(if (animated) 0f else durationMs.toFloat()) }
    LaunchedEffect(animated) {
        if (animated) {
            timeMs.animateTo(
                targetValue = durationMs.toFloat(),
                animationSpec = tween(durationMillis = durationMs, easing = LinearEasing),
            )
        }
    }
    return remember(animated) { EntranceProgress { timeMs.value } }
}

internal val SpringEase = CubicBezierEasing(a = 0.45f, b = 1.45f, c = 0.8f, d = 1f)
internal val SymmetricEase = CubicBezierEasing(a = 0.42f, b = 0f, c = 0.58f, d = 1f)
internal val DecelerateEase = CubicBezierEasing(a = 0f, b = 0.4f, c = 0.3f, d = 1f)
internal val PulseEase = CubicBezierEasing(a = 0.5f, b = 0f, c = 0.5f, d = 1f)