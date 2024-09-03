package com.tangem.core.ui.res

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.tangem.core.ui.windowsize.rememberWindowSizePreview

@Composable
fun TangemThemePreview(
    isDark: Boolean? = null,
    typography: TangemTypography = TangemTheme.typography,
    dimens: TangemDimens = TangemTheme.dimens,
    alwaysShowBottomSheets: Boolean = true,
    rtl: Boolean = false,
    content: @Composable () -> Unit,
) {
    val isDarkTheme = isDark ?: isSystemInDarkTheme()

    CompositionLocalProvider(
        LocalBottomSheetAlwaysVisible provides alwaysShowBottomSheets,
        LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
    ) {
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
}

/**
 * This is used to make the bottom sheet always visible in the Preview and should be `true` only in the Preview.
 * */
val LocalBottomSheetAlwaysVisible: ProvidableCompositionLocal<Boolean> = compositionLocalOf {
    false
}