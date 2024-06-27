package com.tangem.core.ui.res

import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.runtime.*
import com.tangem.core.ui.haptic.HapticManager
import com.tangem.core.ui.haptic.MockHapticManager
// [REDACTED_TODO_COMMENT]
internal const val IS_SYSTEM_IN_DARK_THEME: Boolean = false

@Composable
fun TangemTheme(
    isDark: Boolean = false,
    typography: TangemTypography = TangemTheme.typography,
    dimens: TangemDimens = TangemTheme.dimens,
    hapticManager: HapticManager = MockHapticManager,
    content: @Composable () -> Unit,
) {
    val themeColors = if (isDark) darkThemeColors() else lightThemeColors()
    val rememberedColors = remember { themeColors }
        .also { it.update(themeColors) }

    val shapes = remember { TangemShapes(dimens) }
    MaterialTheme(
        colors = materialThemeColors(colors = themeColors, isDark = isDark),
    ) {
        CompositionLocalProvider(
            LocalTangemColors provides rememberedColors,
            LocalTangemTypography provides typography,
            LocalTangemDimens provides dimens,
            LocalTangemShapes provides shapes,
            LocalIsInDarkTheme provides isDark,
            LocalHapticManager provides hapticManager,
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

    val typography: TangemTypography
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
private fun materialThemeColors(colors: TangemColors, isDark: Boolean): Colors {
    return Colors(
        primary = colors.background.primary,
        primaryVariant = colors.background.secondary,
        secondary = colors.button.primary,
        secondaryVariant = colors.text.accent,
        background = colors.background.primary,
        surface = colors.background.secondary,
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
@ReadOnlyComposable
private fun lightThemeColors(): TangemColors {
    return TangemColors(
        text = TangemColors.Text(
            primary1 = TangemColorPalette.Dark6,
            primary2 = TangemColorPalette.White,
            secondary = TangemColorPalette.Dark2,
            tertiary = TangemColorPalette.Dark1,
            disabled = TangemColorPalette.Light4,
            warning = TangemColorPalette.Amaranth,
            attention = TangemColorPalette.Tangerine,
        ),
        icon = TangemColors.Icon(
            primary1 = TangemColorPalette.Dark6,
            primary2 = TangemColorPalette.White,
            secondary = TangemColorPalette.Dark2,
            informative = TangemColorPalette.Dark1,
            inactive = TangemColorPalette.Light4,
            warning = TangemColorPalette.Amaranth,
            attention = TangemColorPalette.Tangerine,
        ),
        button = TangemColors.Button(
            primary = TangemColorPalette.Dark6,
            secondary = TangemColorPalette.Light2,
            disabled = TangemColorPalette.Light2,
        ),
        background = TangemColors.Background(
            primary = TangemColorPalette.White,
            secondary = TangemColorPalette.Light1,
            tertiary = TangemColorPalette.Light1,
            action = TangemColorPalette.White,
        ),
        control = TangemColors.Control(
            checked = TangemColorPalette.Dark6,
            unchecked = TangemColorPalette.Light2,
            key = TangemColorPalette.White,
        ),
        stroke = TangemColors.Stroke(
            primary = TangemColorPalette.Light2,
            secondary = TangemColorPalette.Dark5,
            transparency = TangemColorPalette.White,
        ),
        field = TangemColors.Field(
            primary = TangemColorPalette.Light1,
            focused = TangemColorPalette.Light2,
        ),
    )
}

@Composable
@ReadOnlyComposable
private fun darkThemeColors(): TangemColors {
    return TangemColors(
        text = TangemColors.Text(
            primary1 = TangemColorPalette.White,
            primary2 = TangemColorPalette.Dark6,
            secondary = TangemColorPalette.Light5,
            tertiary = TangemColorPalette.Dark1,
            disabled = TangemColorPalette.Dark3,
            warning = TangemColorPalette.Flamingo,
            attention = TangemColorPalette.Mustard,
        ),
        icon = TangemColors.Icon(
            primary1 = TangemColorPalette.White,
            primary2 = TangemColorPalette.Dark6,
            secondary = TangemColorPalette.Light5,
            informative = TangemColorPalette.Dark1,
            inactive = TangemColorPalette.Dark3,
            warning = TangemColorPalette.Flamingo,
            attention = TangemColorPalette.Mustard,
        ),
        button = TangemColors.Button(
            primary = TangemColorPalette.Light2,
            secondary = TangemColorPalette.Dark4,
            disabled = TangemColorPalette.Dark5,
        ),
        background = TangemColors.Background(
            primary = TangemColorPalette.Dark6,
            secondary = TangemColorPalette.Black,
            tertiary = TangemColorPalette.Dark6,
            action = TangemColorPalette.Dark5,
        ),
        control = TangemColors.Control(
            checked = TangemColorPalette.Azure,
            unchecked = TangemColorPalette.Dark4,
            key = TangemColorPalette.White,
        ),
        stroke = TangemColors.Stroke(
            primary = TangemColorPalette.Dark4,
            secondary = TangemColorPalette.Dark4,
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
    TangemTypography()
}

private val LocalTangemDimens = staticCompositionLocalOf {
    TangemDimens()
}

private val LocalTangemShapes = staticCompositionLocalOf<TangemShapes> {
    error("No TangemShapes provided")
}

val LocalIsInDarkTheme = staticCompositionLocalOf { false }

val LocalHapticManager = staticCompositionLocalOf<HapticManager> {
    error("No HapticManager provided")
}
