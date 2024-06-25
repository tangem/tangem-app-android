package com.tangem.core.ui.res

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalHapticFeedback
import com.tangem.core.ui.haptic.MockHapticManager
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
            hapticManager = MockHapticManager(LocalHapticFeedback.current),
            content = content,
        )
    }
}