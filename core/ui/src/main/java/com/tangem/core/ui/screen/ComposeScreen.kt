package com.tangem.core.ui.screen

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import com.tangem.core.ui.UiDependencies
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.windowsize.rememberWindowSize
import com.tangem.domain.apptheme.model.AppThemeMode

/**
 * Interface representing a Compose screen with common theming and content composition properties.
 *
 * This interface defines properties and functions that allow a Compose screen to manage its theme,
 * background color, and screen modifier. It also provides a composable function to define the content
 * of the screen.
 */
internal interface ComposeScreen {

    /**
     * The holder for ui dependencies.
     */
    val uiDependencies: UiDependencies

    /**
     * The screen modifier.
     */
    val screenModifier: Modifier
        @Composable
        @ReadOnlyComposable
        get() = Modifier.fillMaxSize()

    /**
     * Composable function to define the content of the screen.
     *
     * @param modifier The modifier to apply to the screen content.
     */
    @Suppress("TopLevelComposableFunctions")
    @Composable
    fun ScreenContent(modifier: Modifier)
}

/**
 * Creates a [ComposeView] with the defined content for the Compose screen.
 *
 * @param context The context.
 * @return A [ComposeView] instance with the defined screen content.
 */
internal fun ComposeScreen.createComposeView(context: Context, activity: Activity): ComposeView {
    return ComposeView(context).apply {
        setContent {
            val appThemeMode by uiDependencies.appThemeModeHolder.appThemeMode
            val windowSize = rememberWindowSize(activity = activity)

            TangemTheme(
                isDark = shouldUseDarkTheme(appThemeMode),
                windowSize = windowSize,
                hapticManager = uiDependencies.hapticManager,
                snackbarHostState = uiDependencies.globalSnackbarHostState,
            ) {
                ScreenContent(modifier = screenModifier)
            }
        }
    }
}

/**
 * Determines whether the dark theme should be used based on the given [AppThemeMode].
 *
 * @param appThemeMode The application theme mode.
 * @return `true` if the dark theme should be used, `false` otherwise.
 */
@Composable
@ReadOnlyComposable
private fun shouldUseDarkTheme(appThemeMode: AppThemeMode): Boolean {
    return when (appThemeMode) {
        AppThemeMode.FORCE_DARK -> true
        AppThemeMode.FORCE_LIGHT -> false
        AppThemeMode.FOLLOW_SYSTEM -> isSystemInDarkTheme()
    }
}