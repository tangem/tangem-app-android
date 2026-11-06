/*
 * Copied from Decompose 3.5.0 (extensions-compose-experimental), commit d36078df71276275adc3af0ac993b575b545f932
 * https://github.com/arkivanov/Decompose
 *

 * Licensed under the Apache License, Version 2.0: https://www.apache.org/licenses/LICENSE-2.0
 */

package com.tangem.core.decompose.ui.stack.animation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.runtime.Stable
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.animation.Direction

/**
 * A scope interface for [StackAnimation], extends [AnimatedVisibilityScope].
 * Allows better UI customization during stack animations.
 */
@ExperimentalDecomposeApi
@Stable
interface StackAnimationScope : AnimatedVisibilityScope {

    /**
     * Returns a [Direction] of the stack animation running for the screen in
     * this scope, or `null` if no animation is currently running.
     *
     * This property is observable in Compose, meaning the composition will be
     * notified when the value changes.
     */
    val stackAnimationDirection: Direction?
}