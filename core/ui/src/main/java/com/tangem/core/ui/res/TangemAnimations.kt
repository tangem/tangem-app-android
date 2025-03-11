package com.tangem.core.ui.res

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*

@Immutable
object TangemAnimations {

    @Composable
    @NonRestartableComposable
    fun horizontalIndicatorAsState(targetFraction: Float): State<Float> {
        return animateFloatAsState(
            targetValue = targetFraction,
            animationSpec = tween(durationMillis = 300),
            label = "Indicator fraction",
        )
    }

    object AnimatedContent {
        fun <S> slide(
            forwardWhen: (initial: S, target: S) -> Boolean,
        ): AnimatedContentTransitionScope<S>.() -> ContentTransform = {
            val direction = if (forwardWhen(initialState, targetState)) {
                AnimatedContentTransitionScope.SlideDirection.End
            } else {
                AnimatedContentTransitionScope.SlideDirection.Start
            }

            slideIntoContainer(towards = direction, animationSpec = tween())
                .togetherWith(slideOutOfContainer(towards = direction, animationSpec = tween()))
        }
    }
}