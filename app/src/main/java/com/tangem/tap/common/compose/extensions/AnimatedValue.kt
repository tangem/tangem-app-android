package com.tangem.tap.common.compose.extensions

import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

/**
[REDACTED_AUTHOR]
 */
typealias AnimatedValue = Pair<Float, Float>

@Composable
fun AnimatedValue.toAnimatable(
    isPaused: Boolean,
    duration: Int,
    easing: Easing = LinearEasing,
): Animatable<Float, AnimationVector1D> {
    return animatable(
        values = this,
        isPaused = isPaused,
        duration = duration,
        easing = easing,
    )
}

@Composable
fun animatable(
    values: AnimatedValue,
    duration: Int,
    isPaused: Boolean = false,
    easing: Easing = LinearEasing,
): Animatable<Float, AnimationVector1D> {
    val animatable = remember { Animatable(values.first) }

    LaunchedEffect(isPaused) {
        if (isPaused) {
            animatable.stop()
        } else {
            animatable.animateTo(
                targetValue = values.second,
                animationSpec = tween(
                    durationMillis = duration,
                    easing = easing
                )
            )
        }
    }
    return animatable
}