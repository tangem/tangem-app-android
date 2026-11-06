package com.tangem.core.decompose.ui.stack.animation

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.animation.Direction
import kotlin.math.abs

/**
 * The app's default screen transition: a short parallax slide combined with a fade.
 *
 * @param slideDirections directions in which the horizontal slide is applied.
 *   `null` (default) means slide in all directions.
 * @param fadeDirections directions in which the alpha fade is applied.
 *   `null` (default) means fade in all directions. Pass `emptySet()` to disable the fade
 *   entirely — useful for screens that own a `hazeEffect` (e.g. WalletTopBar's progressive
 *   blur), where wrapping the screen in an animated `graphicsLayer { alpha = ... }` causes
 *   a visible blink over the blurred region.
 */
@ExperimentalDecomposeApi
@Suppress("MagicNumber")
fun slideAndFade(slideDirections: Set<Direction>? = null, fadeDirections: Set<Direction>? = null): StackAnimator {
    val easing = CubicBezierEasing(a = 0.55f, b = 0.0f, c = 0.0f, d = 1f)

    val slide = stackAnimator(
        animationSpec = tween(durationMillis = 400, easing = easing),
    ) { factor, direction ->
        if (slideDirections == null || slideDirections.contains(direction)) {
            Modifier.parallaxOffsetX(factor)
        } else {
            Modifier
        }
    }

    val fade = directionalFade(
        animationSpec = tween(
            delayMillis = 50,
            durationMillis = 300,
            easing = easing,
        ),
        directions = fadeDirections,
    )

    return slide.plus(fade)
}

/**
 * Like `decompose.fade(...)` but only applies the alpha `graphicsLayer` when `direction`
 * is in [directions]. `null` directions = always fade (matches stock `fade()` behavior).
 * `emptySet()` directions = never fade (modifier passes through untouched).
 *
 * Uses [CompositingStrategy.ModulateAlpha] (not `Offscreen` and not the default `Auto`)
 * because screens that own a `hazeEffect` (e.g. `WalletTopBar`'s progressive blur) render
 * through a `RenderEffect`, which always allocates its own offscreen buffer.
 */
@ExperimentalDecomposeApi
private fun directionalFade(animationSpec: FiniteAnimationSpec<Float>, directions: Set<Direction>?): StackAnimator =
    stackAnimator(animationSpec) { factor, direction ->
        if (directions == null || directions.contains(direction)) {
            Modifier.graphicsLayer {
                alpha = 1f - abs(factor)
                compositingStrategy = CompositingStrategy.ModulateAlpha
            }
        } else {
            Modifier
        }
    }

@Suppress("MagicNumber")
private fun Modifier.parallaxOffsetX(factor: Float): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)

    layout(placeable.width, placeable.height) {
        placeable.placeRelative(x = (placeable.width.toFloat() * factor * 0.15f).toInt(), y = 0)
    }
}