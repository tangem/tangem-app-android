package com.tangem.features.collectibles.impl.packopening.ui.components.confirmorder

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private object ConfirmTiming {

    const val DIP_MS = 100
    const val LAUNCH_MS = 400

    /** The rest of the screen leaves late in the throw, not on the tap. */
    const val EXIT_DELAY_MS = 350
    const val EXIT_MS = 130

    /** Empty confirm screen before the video: 100 + 400 + 200 = 700 ms, the video's black lead-in. */
    const val HOLD_MS = 200
}

/** Ease-out cubic: the dip snaps down and settles. */
private val DIP_EASING = CubicBezierEasing(a = 0.33f, b = 1f, c = 0.68f, d = 1f)

/** Ease-in cubic: the throw is still accelerating when the video takes over. */
private val LAUNCH_EASING = CubicBezierEasing(a = 0.32f, b = 0f, c = 0.67f, d = 0f)

/** Plays the dip, the throw and the hold once [isLaunching] turns true, then reports [onFinish]. */
@Composable
internal fun rememberPackLaunchProgress(isLaunching: Boolean, onFinish: () -> Unit): ConfirmProgress {
    val dip = remember { Animatable(0f) }
    val throwUp = remember { Animatable(0f) }
    val exit = remember { Animatable(0f) }
    val currentOnFinish by rememberUpdatedState(onFinish)

    LaunchedEffect(isLaunching) {
        if (!isLaunching) return@LaunchedEffect

        coroutineScope {
            launch {
                delay(ConfirmTiming.EXIT_DELAY_MS.toLong())
                exit.animateTo(1f, tween(ConfirmTiming.EXIT_MS, easing = FastOutLinearInEasing))
            }
            dip.animateTo(1f, tween(ConfirmTiming.DIP_MS, easing = DIP_EASING))
            throwUp.animateTo(1f, tween(ConfirmTiming.LAUNCH_MS, easing = LAUNCH_EASING))
            delay(ConfirmTiming.HOLD_MS.toLong())
        }
        currentOnFinish()
    }

    return ConfirmProgress(dip = dip.value, launch = throwUp.value, exit = exit.value)
}