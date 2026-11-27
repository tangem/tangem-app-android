package com.tangem.features.collectibles.impl.common.ui.rewardcard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.pow

// The tilt is kept at parity with iOS
private const val MAX_TILT_DEGREES = 14f
private val FollowSpec: SpringSpec<Float> = iosSpring(responseSeconds = 0.25f, dampingFraction = 0.8f)
private val ReturnSpec: SpringSpec<Float> = iosSpring(responseSeconds = 0.45f, dampingFraction = 0.7f)

/** SwiftUI's `spring(response:dampingFraction:)` on a unit mass. */
private fun iosSpring(responseSeconds: Float, dampingFraction: Float): SpringSpec<Float> =
    spring(dampingRatio = dampingFraction, stiffness = (2 * PI / responseSeconds).pow(2).toFloat())

@Composable
internal fun rememberCardTiltState(): CardTiltState {
    val scope = rememberCoroutineScope()
    return remember { CardTiltState(scope) }
}

/**
 * The card held under a finger: the pressed point sinks away from the camera, up to [MAX_TILT_DEGREES] at the
 * card's edges, and a glare lights up under it. Released, the card springs back flat.
 */
@Stable
internal class CardTiltState(private val scope: CoroutineScope) {

    private val animatedRotationX = Animatable(0f)
    private val animatedRotationY = Animatable(0f)
    private val animatedGlare = Animatable(0f)

    /** Turn around the horizontal axis, in degrees. */
    val rotationX: Float get() = animatedRotationX.value

    /** Turn around the vertical axis, in degrees. */
    val rotationY: Float get() = animatedRotationY.value

    /** Glare opacity, from 0 to 1. */
    val glare: Float get() = animatedGlare.value

    var isHeld by mutableStateOf(false)
        private set

    var touch by mutableStateOf(Offset.Zero)
        private set

    fun hold(position: Offset, size: IntSize) {
        isHeld = true
        touch = position

        if (size.width <= 0 || size.height <= 0) return
        animateTo(
            rotationX = -centered(position.y / size.height) * MAX_TILT_DEGREES,
            rotationY = centered(position.x / size.width) * MAX_TILT_DEGREES,
            glare = 1f,
            spec = FollowSpec,
        )
    }

    fun release() {
        isHeld = false
        animateTo(rotationX = 0f, rotationY = 0f, glare = 0f, spec = ReturnSpec)
    }

    private fun animateTo(rotationX: Float, rotationY: Float, glare: Float, spec: SpringSpec<Float>) {
        scope.launch { animatedRotationX.animateTo(targetValue = rotationX, animationSpec = spec) }
        scope.launch { animatedRotationY.animateTo(targetValue = rotationY, animationSpec = spec) }
        scope.launch { animatedGlare.animateTo(targetValue = glare, animationSpec = spec) }
    }

    private fun centered(fraction: Float): Float = (fraction * 2 - 1).coerceIn(-1f, 1f)
}