package com.tangem.core.ui.utils

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

@Composable
fun TangemSharedTransitionLayout(
    modifier: Modifier = Modifier,
    content: @Composable SharedTransitionScope.() -> Unit,
) {
    SharedTransitionLayout(modifier) {
        val sharedTransitionScope = this
        CompositionLocalProvider(
            LocalSharedTransitionScope provides sharedTransitionScope,
        ) {
            content()
        }
    }
}

@Composable
fun ProvideSharedTransitionScope(modifier: Modifier = Modifier, content: @Composable SharedTransitionScope.() -> Unit) {
    val sharedTransitionScope = LocalSharedTransitionScope.current
    Box(modifier) {
        sharedTransitionScope.content()
    }
}

private val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope> {
    error("No SharedTransitionScope provided")
}