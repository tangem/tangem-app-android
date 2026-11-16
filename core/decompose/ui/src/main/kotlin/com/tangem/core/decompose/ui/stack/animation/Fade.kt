/*
 * Copied from Decompose 3.5.0 (extensions-compose-experimental), commit d36078df71276275adc3af0ac993b575b545f932
 * https://github.com/arkivanov/Decompose
 *

 * Licensed under the Apache License, Version 2.0: https://www.apache.org/licenses/LICENSE-2.0
 */

package com.tangem.core.decompose.ui.stack.animation

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.arkivanov.decompose.ExperimentalDecomposeApi
import kotlin.math.abs

/**
 * A simple fading animation. Appearing children's `alpha` is animated from [minAlpha] to 1.0.
 * Disappearing children's `alpha` is animated from 1.0 to [minAlpha].
 */
@ExperimentalDecomposeApi
fun fade(animationSpec: FiniteAnimationSpec<Float> = tween(), minAlpha: Float = 0F): StackAnimator =
    stackAnimator(animationSpec = animationSpec) { factor, _ ->
        Modifier.alpha(getFadeAlpha(factor = factor, minAlpha = minAlpha))
    }

internal fun getFadeAlpha(factor: Float, minAlpha: Float): Float =
    (1F - abs(factor) * (1F - minAlpha)).coerceIn(minimumValue = 0F, maximumValue = 1F)