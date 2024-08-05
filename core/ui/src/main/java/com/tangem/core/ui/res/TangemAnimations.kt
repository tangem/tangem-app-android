package com.tangem.core.ui.res

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*

@Immutable
object TangemAnimations {

    val transitionSpecs = TransitionSpecs

    @Composable
    @NonRestartableComposable
    fun horizontalIndicatorAsState(targetFraction: Float): State<Float> {
        return animateFloatAsState(
            targetValue = targetFraction,
            animationSpec = tween(durationMillis = 300),
            label = "Indicator fraction",
        )
    }

    @Immutable
    object TransitionSpecs {

        @Stable
        val textChange: ContentTransform =
            fadeIn(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy)) togetherWith
                fadeOut(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy))
    }
}
