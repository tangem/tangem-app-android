package com.tangem.tap.routing.transitions

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import com.arkivanov.decompose.extensions.compose.stack.animation.*
import com.tangem.common.routing.AppRoute

object RoutingTransitionAnimationFactory {

    @Suppress("MagicNumber")
    fun create(appRoute: AppRoute): StackAnimator {
        return when (appRoute) {
            is AppRoute.Onboarding,
            is AppRoute.Welcome,
            is AppRoute.Home,
            -> fade(tween(400)).plus(scale(tween(400)))
            is AppRoute.Wallet,
            -> slideAndFade(directions = setOf(Direction.ENTER_BACK, Direction.EXIT_BACK))
                .plus(
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

    @Suppress("MagicNumber")
    private fun slideAndFade(directions: Set<Direction>? = null): StackAnimator {
        val easing = CubicBezierEasing(a = 0.55f, b = 0.0f, c = 0.0f, d = 1f)

        return stackAnimator(
            animationSpec = tween(durationMillis = 400, easing = easing),
        ) { factor, direction, content ->
            content(
                if (directions == null || directions.contains(direction)) {
                    Modifier.offsetXFactor(factor)
                } else {
                    Modifier
                },
            )
        }.plus(
            fade(
                animationSpec = tween(
                    delayMillis = 50,
                    durationMillis = 300,
                    easing = easing,
                ),
            ),
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