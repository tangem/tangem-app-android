package com.tangem.core.ui.extensions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color

/**
 * Utility class for keeping themed color reference from app theme.
 *
 * It is necessary to use [Immutable] annotation for runtime stability.
 *
 * @property value color provider from theme
 */
@Deprecated("Use TextReference with applied SpanStyleReference for colored text or ColorReference2 for color.")
@Immutable
data class ColorReference(val value: @Composable () -> Color)

/**
 * Creates a [ColorReference] using a themed color from the app theme with a lambda.
 *
 * @param value The color provider from theme.
 * @return A [ColorReference] representing the themed color.
 */
fun themedColor(value: @Composable () -> Color): ColorReference {
    return ColorReference(value)
}

/**
 * Resolves [ColorReference] to [Color]
 */
@Composable
fun ColorReference.resolveReference(): Color {
    return value()
}

/**
 * Utility functional interface for keeping themed [Color] reference from app theme.
 * It is necessary to use [Stable] annotation for runtime stability.
 */
@Stable
@FunctionalInterface
fun interface ColorReference2 {

    @ReadOnlyComposable
    @Composable
    operator fun invoke(): Color
}