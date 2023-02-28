package com.tangem.tap.common.compose

import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Used for disable ripple if button is enable = false
 */
@Composable
fun ToggledRippleTheme(
    isEnabled: Boolean,
    content: @Composable () -> Unit,
) {
    val theme = LocalRippleTheme provides if (isEnabled) LocalRippleTheme.current else NoRippleTheme()
    CompositionLocalProvider(theme) { content() }
}
