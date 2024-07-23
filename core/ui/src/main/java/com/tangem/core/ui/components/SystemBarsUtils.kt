package com.tangem.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.tangem.core.ui.res.LocalIsInDarkTheme

/**
 * Provides the ability to set a scrim for 3-button navigation
 *
 * Automatically makes navigation bar transparent when the composable is disposed.
 *
 * Usage example: [com.tangem.feature.tokendetails.presentation.TokenDetailsFragment]
 */
@Composable
fun NavigationBar3ButtonsScrim() {
    val systemUiController = rememberSystemUiController()
    val isDarkTheme = LocalIsInDarkTheme.current
    SideEffect {
        systemUiController.isNavigationBarContrastEnforced = true
    }
    LaunchedEffect(systemUiController.isNavigationBarContrastEnforced) {
        if (systemUiController.isNavigationBarContrastEnforced.not()) {
            systemUiController.isNavigationBarContrastEnforced = true
        }
    }
    DisposableEffect(isDarkTheme) {
        onDispose {
            systemUiController.isNavigationBarContrastEnforced = false
        }
    }
}

/**
 * Provides the ability to set dark/light icons in cases where the darkness of the screen differs from that
 * provided in [TangemTheme].
 *
 * Automatically returns the icon colors to their original state when the composable is disposed.
 *
 * Usage example: [com.tangem.feature.qrscanning.QrScanningFragment]
 */
@Composable
fun SystemBarsIconsDisposable(darkIcons: Boolean, isNavigationBarContrastEnforced: Boolean = false) {
    val systemUiController = rememberSystemUiController()

    SideEffect {
        systemUiController.systemBarsDarkContentEnabled = darkIcons
        systemUiController.isNavigationBarContrastEnforced = isNavigationBarContrastEnforced
    }

    val isDarkTheme = LocalIsInDarkTheme.current

    DisposableEffect(isDarkTheme) {
        onDispose {
            systemUiController.systemBarsDarkContentEnabled = !isDarkTheme
            systemUiController.isNavigationBarContrastEnforced = false
        }
    }
}
