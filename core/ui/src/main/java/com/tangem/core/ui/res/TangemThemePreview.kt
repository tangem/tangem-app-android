package com.tangem.core.ui.res

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable

@Composable
fun TangemThemePreview(
    isDark: Boolean? = null,
    typography: TangemTypography = TangemTheme.typography,
    dimens: TangemDimens = TangemTheme.dimens,
    content: @Composable () -> Unit,
) {
    val isDarkTheme = isDark ?: isSystemInDarkTheme()

    TangemTheme(
        isDark = isDarkTheme,
        typography = typography,
        dimens = dimens,
        content = content,
    )
}