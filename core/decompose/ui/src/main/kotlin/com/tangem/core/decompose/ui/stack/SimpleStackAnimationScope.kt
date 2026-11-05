/*
 * Copied from Decompose 3.5.0 (extensions-compose-experimental), commit d36078df71276275adc3af0ac993b575b545f932
 * https://github.com/arkivanov/Decompose
 *

 * Licensed under the Apache License, Version 2.0: https://www.apache.org/licenses/LICENSE-2.0
 */

package com.tangem.core.decompose.ui.stack

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.Transition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.tangem.core.decompose.ui.stack.animation.StackAnimationScope
import com.arkivanov.decompose.extensions.compose.stack.animation.Direction

@Composable
internal fun WithStackAnimationScope(
    direction: Direction?,
    transition: Transition<EnterExitState>,
    block: @Composable StackAnimationScope.() -> Unit,
) {
    val directionState = rememberUpdatedState(direction)
    val transitionState = rememberUpdatedState(transition)
    val scope = remember(directionState, transitionState) { SimpleStackAnimationScope(directionState, transitionState) }
    scope.block()
}

private class SimpleStackAnimationScope(
    directionState: State<Direction?>,
    transitionState: State<Transition<EnterExitState>>,
) : StackAnimationScope {
    override val stackAnimationDirection: Direction? by directionState::value
    override val transition: Transition<EnterExitState> by transitionState::value
}