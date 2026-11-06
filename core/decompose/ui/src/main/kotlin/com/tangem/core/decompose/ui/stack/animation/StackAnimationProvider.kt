/*
 * Copied from Decompose 3.5.0 (extensions-compose-experimental), commit d36078df71276275adc3af0ac993b575b545f932
 * https://github.com/arkivanov/Decompose
 *

 * Licensed under the Apache License, Version 2.0: https://www.apache.org/licenses/LICENSE-2.0
 */

package com.tangem.core.decompose.ui.stack.animation

import androidx.compose.runtime.compositionLocalOf
import com.arkivanov.decompose.ExperimentalDecomposeApi

@ExperimentalDecomposeApi
interface StackAnimationProvider {
    fun <C : Any, T : Any> provide(): StackAnimation<C, T>?
}

@ExperimentalDecomposeApi
val LocalStackAnimationProvider =
    compositionLocalOf<StackAnimationProvider> {
        object : StackAnimationProvider {
            override fun <C : Any, T : Any> provide(): StackAnimation<C, T>? = null
        }
    }