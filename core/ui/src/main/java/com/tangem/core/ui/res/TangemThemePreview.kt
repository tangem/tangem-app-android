package com.tangem.core.ui.res

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import com.tangem.core.ui.windowsize.rememberWindowSizePreview

@Composable
fun TangemThemePreview(
    isDark: Boolean? = null,
    typography: TangemTypography = TangemTheme.typography,
    dimens: TangemDimens = TangemTheme.dimens,
    content: @Composable () -> Unit,
) {
    val isDarkTheme = isDark ?: isSystemInDarkTheme()

    BoxWithConstraints {
        TangemTheme(
            isDark = isDarkTheme,
            typography = typography,
            dimens = dimens,
            windowSize = rememberWindowSizePreview(maxWidth, maxHeight),
            content = content,
        )
    }
}