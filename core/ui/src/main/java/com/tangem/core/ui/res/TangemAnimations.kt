package com.tangem.core.ui.res

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
        // TODO add more transition specs
    }
}