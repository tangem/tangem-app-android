/*
 * Copied from Decompose 3.5.0 (extensions-compose-experimental), commit d36078df71276275adc3af0ac993b575b545f932
 * https://github.com/arkivanov/Decompose
 *

 * Licensed under the Apache License, Version 2.0: https://www.apache.org/licenses/LICENSE-2.0
 */

package com.tangem.core.decompose.ui.stack.animation

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.Child
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.tangem.core.decompose.ui.stack.WithStackAnimationScope
import com.arkivanov.decompose.router.stack.ChildStack

@ExperimentalDecomposeApi
internal fun <C : Any, T : Any> emptyStackAnimation(): StackAnimation<C, T> = EmptyStackAnimation()

/*
 * Can't be anonymous. See:
 * https://github.com/JetBrains/compose-jb/issues/2688
 * https://github.com/JetBrains/compose-jb/issues/2612
 */
@ExperimentalDecomposeApi
private class EmptyStackAnimation<C : Any, T : Any> : StackAnimation<C, T> {

    @Composable
    override fun invoke(
        stack: ChildStack<C, T>,
        modifier: Modifier,
        content: @Composable StackAnimationScope.(child: Child.Created<C, T>) -> Unit,
    ) {
        val transitionState = remember { MutableTransitionState(EnterExitState.Visible) }
        val transition = rememberTransition(transitionState)

        WithStackAnimationScope(direction = null, transition = transition) {
            Box(modifier = modifier) {
                content(stack.active)
            }
        }
    }
}