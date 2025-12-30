package com.tangem.core.ui.extensions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.SpanStyle

/**
 * Utility functional interface for keeping themed [SpanStyle] reference from app theme.
 * It is necessary to use [Stable] annotation for runtime stability.
 */
@Stable
@FunctionalInterface
fun interface SpanStyleReference {

    @ReadOnlyComposable
    @Composable
    operator fun invoke(): SpanStyle
}