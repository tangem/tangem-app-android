package com.tangem.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.google.accompanist.systemuicontroller.SystemUiController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.tangem.core.ui.res.LocalIsInDarkTheme

val LocalSystemBarsIconsController = staticCompositionLocalOf<SystemBarsIconsController> {
    error("No SystemBarsIconsController provided")
}

class SystemBarsIconsController(private val systemUiController: SystemUiController) {
    private var count by mutableIntStateOf(0)

    fun setIcons(darkIcons: Boolean, isNavigationBarContrastEnforced: Boolean) {
        if (count == 0) {
            systemUiController.systemBarsDarkContentEnabled = darkIcons
            systemUiController.isNavigationBarContrastEnforced = isNavigationBarContrastEnforced
        }
        count++
    }

    fun restoreIcons(isDarkTheme: Boolean) {
        count--
        if (count == 0) {
            systemUiController.systemBarsDarkContentEnabled = !isDarkTheme
            systemUiController.isNavigationBarContrastEnforced = false
        }
    }
}

@Composable
fun ProvideSystemBarsIconsController(content: @Composable () -> Unit) {
    val systemUiController = rememberSystemUiController()
    val controller = remember(systemUiController) { SystemBarsIconsController(systemUiController) }

    CompositionLocalProvider(
        LocalSystemBarsIconsController provides controller,
        content = content,
    )
}

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
    val controller = LocalSystemBarsIconsController.current
    val isDarkTheme = LocalIsInDarkTheme.current

    SideEffect {
        controller.setIcons(darkIcons, isNavigationBarContrastEnforced)
    }

    DisposableEffect(isDarkTheme) {
        onDispose {
            controller.restoreIcons(isDarkTheme)
        }
    }
}