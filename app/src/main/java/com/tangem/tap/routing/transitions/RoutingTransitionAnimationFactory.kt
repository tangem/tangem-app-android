package com.tangem.tap.routing.transitions

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import com.arkivanov.decompose.extensions.compose.stack.animation.*
import com.tangem.common.routing.AppRoute
import kotlin.math.abs

object RoutingTransitionAnimationFactory {

    @Suppress("MagicNumber")
    fun create(appRoute: AppRoute): StackAnimator {
        return when (appRoute) {
            is AppRoute.Welcome,
            is AppRoute.Home,
            -> fade(tween(400)).plus(scale(tween(400)))
            is AppRoute.Wallet,
            -> slideAndFade(
                slideDirections = setOf(Direction.ENTER_BACK, Direction.EXIT_BACK),
                fadeDirections = emptySet(),
            ).plus(
                scaleWithDirection(
                    directions = setOf(Direction.ENTER_FRONT, Direction.EXIT_FRONT),
                    animationSpec = tween(400),
                ),
            )
            else -> slideAndFade()
        }
    }

    private fun scaleWithDirection(
        directions: Set<Direction>,
        animationSpec: FiniteAnimationSpec<Float> = tween(),
        frontFactor: Float = 1.15F,
        backFactor: Float = 0.95F,
    ): StackAnimator = stackAnimator(animationSpec = animationSpec) { factor, direction, content ->
        content(
            Modifier.graphicsLayer {
                val scaleFactor = if (directions.contains(direction)) {
                    if (factor >= 0F) {
                        factor * (frontFactor - 1F) + 1F
                    } else {
                        factor * (1F - backFactor) + 1F
                    }
                } else {
                    1f
                }
                scaleX = scaleFactor
                scaleY = scaleFactor
            },
        )
    }

    /**
     * @param slideDirections directions in which the horizontal slide is applied.
     *   `null` (default) means slide in all directions.
     * @param fadeDirections directions in which the alpha fade is applied.
     *   `null` (default) means fade in all directions. Pass `emptySet()` to disable the fade
     *   entirely — useful for screens that own a `hazeEffect` (e.g. WalletTopBar's progressive
     *   blur), where wrapping the screen in an animated `graphicsLayer { alpha = ... }` causes
     *   a visible blink over the blurred region.
     */
    @Suppress("MagicNumber")
    private fun slideAndFade(
        slideDirections: Set<Direction>? = null,
        fadeDirections: Set<Direction>? = null,
    ): StackAnimator {
        val easing = CubicBezierEasing(a = 0.55f, b = 0.0f, c = 0.0f, d = 1f)

        val slide = stackAnimator(
            animationSpec = tween(durationMillis = 400, easing = easing),
        ) { factor, direction, content ->
            content(
                if (slideDirections == null || slideDirections.contains(direction)) {
                    Modifier.offsetXFactor(factor)
                } else {
                    Modifier
                },
            )
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
    private fun directionalFade(
        animationSpec: FiniteAnimationSpec<Float>,
        directions: Set<Direction>?,
    ): StackAnimator = stackAnimator(animationSpec) { factor, direction, content ->
        content(
            if (directions == null || directions.contains(direction)) {
                Modifier.graphicsLayer {
                    alpha = 1f - abs(factor)
                    compositingStrategy = CompositingStrategy.ModulateAlpha
                }
            } else {
                Modifier
            },
        )
    }

    @Suppress("MagicNumber")
    private fun Modifier.offsetXFactor(factor: Float): Modifier = layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)

        layout(placeable.width, placeable.height) {
            placeable.placeRelative(x = (placeable.width.toFloat() * factor * 0.15f).toInt(), y = 0)
        }
    }
}