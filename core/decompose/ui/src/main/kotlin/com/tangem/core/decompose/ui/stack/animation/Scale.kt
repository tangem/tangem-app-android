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
import androidx.compose.ui.draw.scale
import com.arkivanov.decompose.ExperimentalDecomposeApi

/**
 * A simple scaling animation. Front (above) children are scaling from [frontFactor] to 1.0.
 * Back (below) children are scaling from 1.0 to [backFactor].
 */
@ExperimentalDecomposeApi
fun scale(
    animationSpec: FiniteAnimationSpec<Float> = tween(),
    frontFactor: Float = 1.15F,
    backFactor: Float = 0.95F,
): StackAnimator = stackAnimator(animationSpec = animationSpec) { factor, _ ->
    Modifier.scale(
        if (factor >= 0F) {
            factor * (frontFactor - 1F) + 1F
        } else {
            factor * (1F - backFactor) + 1F
        },
    )
}