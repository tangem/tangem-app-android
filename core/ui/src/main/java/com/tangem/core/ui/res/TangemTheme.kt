package com.tangem.core.ui.res

import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import com.google.accompanist.systemuicontroller.rememberSystemUiController

internal val isSystemInDarkTheme: Boolean = false // TODO: use isSystemInDarkTheme() for automatic color detection

@Composable
fun TangemTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme,
    content: @Composable () -> Unit,
) {
    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setSystemBarsColor(color = if (isDarkTheme) Black else Light1, darkIcons = !isDarkTheme)
    }

    MaterialTheme(
        colors = if (isDarkTheme) DarkColors else LightColors,
        typography = TangemTypography,
        content = content,
    )
}

private val LightColors = lightColors(
    primary = Light1,
    primaryVariant = Light1,
    secondary = White,
    background = Light1,
    surface = White,
    error = Amaranth,
    onPrimary = Dark6,
    onSecondary = Dark6,
    onBackground = Dark6,
    onSurface = Dark6,
)

private val DarkColors = darkColors(
    primary = Black,
    primaryVariant = Black,
    secondary = Dark6,
    background = Black,
    surface = Dark6,
    error = Amaranth,
    onPrimary = White,
    onSecondary = White,
    onBackground = White,
    onSurface = White,
)