package com.tangem.core.ui.res

import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

// TODO: use isSystemInDarkTheme() for automatic color detection
internal const val IS_SYSTEM_IN_DARK_THEME: Boolean = false

@Composable
fun TangemTheme(
    isDark: Boolean = false,
    typography: Typography = TangemTheme.typography,
    dimens: TangemDimens = TangemTheme.dimens,
    content: @Composable () -> Unit,
) {
    val themeColors = if (isDark) darkThemeColors() else lightThemeColors()
    val rememberedColors = remember { themeColors }
        .also { it.update(themeColors) }

    val shapes = remember { TangemShapes(dimens) }

    MaterialTheme(
        colors = materialThemeColors(colors = themeColors, isDark = isDark),
        typography = typography,
    ) {
        CompositionLocalProvider(
            LocalTangemColors provides rememberedColors,
            LocalTangemTypography provides typography,
            LocalTangemDimens provides dimens,
            LocalTangemShapes provides shapes,
        ) {
            ProvideTextStyle(
                value = TangemTheme.typography.body1,
                content = content,
            )
        }
    }
}

object TangemTheme {
    val colors: TangemColors
        @Composable
        @ReadOnlyComposable
        get() = LocalTangemColors.current

    val typography: Typography
        @Composable
        @ReadOnlyComposable
        get() = LocalTangemTypography.current

    val dimens: TangemDimens
        @Composable
        @ReadOnlyComposable
        get() = LocalTangemDimens.current

    val shapes: TangemShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalTangemShapes.current
}

@Stable
@Composable
private fun materialThemeColors(
    colors: TangemColors,
    isDark: Boolean,
): Colors {
    return Colors(
        primary = colors.background.primary,
        primaryVariant = colors.background.secondary,
        secondary = colors.button.primary,
        secondaryVariant = colors.text.accent,
        background = colors.background.primary,
        surface = colors.background.plain,
        error = colors.text.warning,
        onPrimary = colors.text.primary1,
        onSecondary = colors.text.primary1,
        onBackground = colors.text.primary1,
        onSurface = colors.text.primary1,
        onError = colors.text.primary2,
        isLight = !isDark,
    )
}

@Composable
private fun lightThemeColors(): TangemColors {
    return TangemColors(
        text = TangemColors.Text(
            primary1 = TangemColorPalette.Dark6,
            primary2 = TangemColorPalette.White,
            secondary = TangemColorPalette.Dark2,
            tertiary = TangemColorPalette.Dark1,
            disabled = TangemColorPalette.Light4,
        ),
        icon = TangemColors.Icon(
            primary1 = TangemColorPalette.Black,
            primary2 = TangemColorPalette.White,
            secondary = TangemColorPalette.Dark2,
            informative = TangemColorPalette.Light5,
            inactive = TangemColorPalette.Light4,
        ),
        button = TangemColors.Button(
            primary = TangemColorPalette.Dark6,
            secondary = TangemColorPalette.Light2,
            disabled = TangemColorPalette.Light2,
            positiveDisabled = TangemColorPalette.MagicMint,
        ),
        background = TangemColors.Background(
            primary = TangemColorPalette.White,
            secondary = TangemColorPalette.Light1,
            plain = TangemColorPalette.White,
            action = TangemColorPalette.Black,
            fade = TangemColorPalette.White,
        ),
        control = TangemColors.Control(
            checked = TangemColorPalette.Meadow,
            unchecked = TangemColorPalette.Light2,
            key = TangemColorPalette.White,
        ),
        stroke = TangemColors.Stroke(
            primary = TangemColorPalette.Light2,
            secondary = TangemColorPalette.Dark4,
            transparency = TangemColorPalette.White,
        ),
        field = TangemColors.Field(
            primary = TangemColorPalette.Light1,
            focused = TangemColorPalette.Light2,
        ),
    )
}

@Composable
private fun darkThemeColors(): TangemColors {
    return TangemColors(
        text = TangemColors.Text(
            primary1 = TangemColorPalette.White,
            primary2 = TangemColorPalette.Dark6,
            secondary = TangemColorPalette.Light5,
            tertiary = TangemColorPalette.Dark1,
            disabled = TangemColorPalette.Dark3,
        ),
        icon = TangemColors.Icon(
            primary1 = TangemColorPalette.White,
            primary2 = TangemColorPalette.Dark6,
            secondary = TangemColorPalette.Dark1,
            informative = TangemColorPalette.Dark2,
            inactive = TangemColorPalette.Dark4,
        ),
        button = TangemColors.Button(
            primary = TangemColorPalette.Light1,
            secondary = TangemColorPalette.Dark5,
            disabled = TangemColorPalette.Dark6,
            positiveDisabled = TangemColorPalette.DarkGreen,
        ),
        background = TangemColors.Background(
            primary = TangemColorPalette.Dark6,
            secondary = TangemColorPalette.Black,
            plain = TangemColorPalette.Black,
            action = TangemColorPalette.Light4,
            fade = TangemColorPalette.Black,
        ),
        control = TangemColors.Control(
            checked = TangemColorPalette.Meadow,
            unchecked = TangemColorPalette.Dark4,
            key = TangemColorPalette.Light1,
        ),
        stroke = TangemColors.Stroke(
            primary = TangemColorPalette.Dark5,
            secondary = TangemColorPalette.Dark1,
            transparency = TangemColorPalette.Dark6,
        ),
        field = TangemColors.Field(
            primary = TangemColorPalette.Dark5,
            focused = TangemColorPalette.Dark4,
        ),
    )
}

private val LocalTangemColors = staticCompositionLocalOf<TangemColors> {
    error("No TangemColors provided")
}

private val LocalTangemTypography = staticCompositionLocalOf {
    TangemTypography
}

private val LocalTangemDimens = staticCompositionLocalOf {
    TangemDimens()
}

private val LocalTangemShapes = staticCompositionLocalOf<TangemShapes> {
    error("No TangemShapes provided")
}
