package com.tangem.feature.tester.presentation.storybook.page.ds

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.tangem.core.ui.res.TangemTheme

/**
 * Makes both system bars transparent while an edge-to-edge story page is shown and restores the
 * tester's opaque bars on leave. Icons are dark in light theme and light in dark theme.
 */
@Composable
internal fun TransparentSystemBarsEffect() {
    val systemUiController = rememberSystemUiController()
    val restoreColor = TangemTheme.colors.background.secondary
    val isDarkIcons = !isSystemInDarkTheme()
    SideEffect {
        systemUiController.setStatusBarColor(color = Color.Transparent, darkIcons = isDarkIcons)
        systemUiController.setNavigationBarColor(
            color = Color.Transparent,
            darkIcons = isDarkIcons,
            navigationBarContrastEnforced = false,
        )
    }
    DisposableEffect(systemUiController) {
        onDispose { systemUiController.setSystemBarsColor(color = restoreColor) }
    }
}