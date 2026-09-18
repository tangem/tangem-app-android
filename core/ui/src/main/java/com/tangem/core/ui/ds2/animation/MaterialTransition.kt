package com.tangem.core.ui.ds2.animation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.tangem.core.ui.res.LocalMaterialRenderingEnabled

/**
 * Runs [content] with the material chrome dropped — no translucent fill, no gradient stroke, no
 * drop shadow — while this enter/exit transition animates.
 */
@Composable
fun AnimatedVisibilityScope.WithoutMaterialWhileAnimating(content: @Composable () -> Unit) {
    // currentState != targetState, not isRunning alone: the transition is still idle on the frame it
    // is composed on, and that is the frame the bare outline would show up on.
    // AND-ed with the current value so an outer animation keeps the chrome off while an inner one idles.
    val isAnimating = transition.isRunning || transition.currentState != transition.targetState
    CompositionLocalProvider(
        LocalMaterialRenderingEnabled provides (LocalMaterialRenderingEnabled.current && !isAnimating),
    ) {
        content()
    }
}