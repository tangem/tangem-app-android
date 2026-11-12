package com.tangem.tap.routing.transitions

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.animation.Direction
import com.tangem.core.decompose.ui.stack.animation.StackAnimator
import com.tangem.core.decompose.ui.stack.animation.fade
import com.tangem.core.decompose.ui.stack.animation.plus
import com.tangem.core.decompose.ui.stack.animation.scale
import com.tangem.core.decompose.ui.stack.animation.slideAndFade
import com.tangem.core.decompose.ui.stack.animation.stackAnimator
import com.tangem.common.routing.AppRoute

@OptIn(ExperimentalDecomposeApi::class)
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
            is AppRoute.CurrencyDetails,
            -> slideAndFade(
                // Token details owns a hazeEffect top bar. Alpha-animating it while it is covered by
                // or revealed from another screen blinks black over the blurred region — the same
                // artifact WalletTopBar hit above. Keep the fade only for the FRONT directions.
                fadeDirections = setOf(Direction.ENTER_FRONT, Direction.EXIT_FRONT),
            )
            else -> slideAndFade()
        }
    }

    private fun scaleWithDirection(
        directions: Set<Direction>,
        animationSpec: FiniteAnimationSpec<Float> = tween(),
        frontFactor: Float = 1.15F,
        backFactor: Float = 0.95F,
    ): StackAnimator = stackAnimator(animationSpec = animationSpec) { factor, direction ->
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
        }
    }
}