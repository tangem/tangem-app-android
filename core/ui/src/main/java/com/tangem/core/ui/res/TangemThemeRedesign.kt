@file:Suppress("LongMethod")

package com.tangem.core.ui.res

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*

/**
 * Provides additional theming for redesigned components.
 * Used together with [TangemTheme].
 * @param content Composable content where the theme is applied.
 */
@Composable
fun TangemThemeRedesign(content: @Composable () -> Unit) {
    val themeColors = if (LocalIsInDarkTheme.current) darkThemeColors() else lightThemeColors(redesign = true)
    val rememberedColors = remember { themeColors }
        .apply { update(themeColors) }
    val rootBackgroundColor = rememberedColors.background.secondary

    MaterialTheme(
        colorScheme = tangemColorScheme(colors = themeColors),
    ) {
        CompositionLocalProvider(
            LocalTangemColors provides themeColors,
            LocalTangemColors2 provides if (LocalIsInDarkTheme.current) darkThemeColors2() else lightThemeColors2(),
            LocalTangemTypography2 provides TangemTypography2(InterFamily),
            LocalRootBackgroundColor provides remember(rootBackgroundColor) { mutableStateOf(rootBackgroundColor) },
        ) {
            content()
        }
    }
}

@Composable
@ReadOnlyComposable
private fun lightThemeColors2(): TangemColors2 {
    val text = TangemColors2.Text(
        neutral = TangemColors2.Text.Neutral(
            primary = TangemColorPalette.Dark6,
            primaryInverted = TangemColorPalette.White,
            secondary = TangemColorPalette.Dark2,
            tertiary = TangemColorPalette.Dark3,
            primaryInvertedConstant = TangemColorPalette.White,
        ),
        status = TangemColors2.Text.Status(
            disabled = TangemColorPalette.Light4,
            accent = TangemColorPalette.Azure,
            warning = TangemColorPalette.Amaranth,
            attention = TangemColorPalette.Tangerine,
            positive = TangemColorPalette.Green,
        ),
    )
    val graphic = TangemColors2.Graphic(
        neutral = TangemColors2.Graphic.Neutral(
            primary = TangemColorPalette.Dark6,
            primaryInverted = TangemColorPalette.White,
            secondary = TangemColorPalette.Dark2,
            tertiary = TangemColorPalette.Dark3,
            quaternary = TangemColorPalette.Light4,
            primaryInvertedConstant = TangemColorPalette.White,
            tertiaryConstant = TangemColorPalette.Dark3,
        ),
        status = TangemColors2.Graphic.Status(
            accent = TangemColorPalette.Azure,
            warning = TangemColorPalette.Amaranth,
            attention = TangemColorPalette.Tangerine,
        ),
    )
    val border = TangemColors2.Border(
        neutral = TangemColors2.Border.Neutral(
            primary = TangemColorPalette.Light3,
            secondary = TangemColorPalette.Light5,
        ),
        status = TangemColors2.Border.Status(
            accent = TangemColorPalette.Azure,
            warning = TangemColorPalette.Amaranth,
            attention = TangemColorPalette.Tangerine,
        ),
    )
    val overlay = TangemColors2.Overlay(
        overlayPrimary = TangemColorPalette.Overlay1,
        overlaySecondary = TangemColorPalette.Overlay2,
    )
    val fill = TangemColors2.Fill(
        neutral = TangemColors2.Fill.Neutral(
            primary = TangemColorPalette.Dark6,
            primaryInverted = TangemColorPalette.White,
            primaryInvertedConstant = TangemColorPalette.White,
            secondary = TangemColorPalette.Dark3,
            tertiaryConstant = TangemColorPalette.Dark3,
            quaternary = TangemColorPalette.Light4,
        ),
        status = TangemColors2.Fill.Status(
            accent = TangemColorPalette.Azure,
            warning = TangemColorPalette.Amaranth,
            attention = TangemColorPalette.Tangerine,
        ),
    )
    val button = TangemColors2.Button(
        backgroundPrimary = TangemColorPalette.Dark6,
        backgroundSecondary = TangemColorPalette.Dark_10,
        backgroundDisabled = TangemColorPalette.Light3,
        backgroundPositive = TangemColorPalette.Azure,
        backgroundPrimaryInverse = TangemColorPalette.White,
        textSecondary = TangemColorPalette.Dark6,
        textPrimary = TangemColorPalette.Light2,
        textDisabled = text.neutral.tertiary,
        iconPrimary = TangemColorPalette.Dark6,
        iconSecondary = TangemColorPalette.Light1V2,
        iconDisabled = TangemColorPalette.Light2,
        borderPrimary = TangemColorPalette.Dark6,
    )
    val surface = TangemColors2.Surface(
        level1 = TangemColorPalette.White,
        level2 = TangemColorPalette.Light1V2,
        level3 = TangemColorPalette.Light1V2,
        level4 = TangemColorPalette.White,
    )
    val controls = TangemColors2.Controls(
        backgroundChecked = TangemColorPalette.Dark6,
        backgroundDefault = TangemColorPalette.Light2,
        iconDefault = TangemColorPalette.White,
        iconDisabled = TangemColorPalette.White,
    )
    val field = TangemColors2.Field(
        backgroundDefault = TangemColorPalette.Light1V2,
        backgroundFocused = TangemColorPalette.Light3,
        textPlaceholder = text.neutral.secondary,
        textDefault = text.neutral.primary,
        textDisabled = text.neutral.tertiary,
        iconDefault = graphic.neutral.tertiary,
        iconDisabled = graphic.neutral.quaternary,
        textInvalid = text.status.warning,
        borderInvalid = border.status.warning,
    )
    val skeleton = TangemColors2.Skeleton(
        backgroundPrimary = TangemColorPalette.Light1V2,
    )
    val markers = TangemColors2.Markers(
        backgroundSolidGray = TangemColorPalette.Light3,
        backgroundDisabled = TangemColorPalette.Light3,
        backgroundSolidBlue = TangemColorPalette.Azure,
        textGray = TangemColorPalette.Dark2,
        textDisabled = text.neutral.tertiary,
        iconGray = TangemColorPalette.Dark1,
        iconDisabled = TangemColorPalette.Light2,
        borderGray = TangemColorPalette.Light3,
        backgroundTintedBlue = TangemColorPalette.Azure.copy(alpha = 0.1f),
        textBlue = text.status.accent,
        backgroundSolidRed = TangemColorPalette.Amaranth,
        backgroundTintedRed = TangemColorPalette.Amaranth.copy(alpha = 0.1f),
        iconBlue = TangemColorPalette.Azure,
        iconRed = TangemColorPalette.Amaranth,
        textRed = TangemColorPalette.Amaranth,
        backgroundTintedGray = TangemColorPalette.Dark6.copy(alpha = 0.1f),
        borderTintedBlue = TangemColorPalette.Azure.copy(alpha = 0.1f),
        borderTintedRed = TangemColorPalette.Amaranth.copy(alpha = 0.1f),
    )
    val tabs = TangemColors2.Tabs(
        textPrimary = TangemColorPalette.Light2,
        textSecondary = TangemColorPalette.Dark4,
        textTertiary = TangemColorPalette.Black,
        backgroundPrimary = TangemColorPalette.Dark6,
        backgroundSecondary = TangemColorPalette.Dark_10,
        backgroundTertiary = TangemColorPalette.White,
        backgroundQuaternary = TangemColorPalette.Dark_20,
    )
    return TangemColors2(
        text = text,
        graphic = graphic,
        border = border,
        overlay = overlay,
        fill = fill,
        button = button,
        surface = surface,
        controls = controls,
        field = field,
        skeleton = skeleton,
        markers = markers,
        tabs = tabs,
    )
}

@Composable
@ReadOnlyComposable
private fun darkThemeColors2(): TangemColors2 {
    val text = TangemColors2.Text(
        neutral = TangemColors2.Text.Neutral(
            primary = TangemColorPalette.White,
            primaryInverted = TangemColorPalette.Dark6,
            secondary = TangemColorPalette.Light5,
            tertiary = TangemColorPalette.Dark1,
            primaryInvertedConstant = TangemColorPalette.White,
        ),
        status = TangemColors2.Text.Status(
            disabled = TangemColorPalette.Dark3,
            accent = TangemColorPalette.Azure,
            warning = TangemColorPalette.Flamingo,
            attention = TangemColorPalette.Mustard,
            positive = TangemColorPalette.Green,
        ),
    )
    val graphic = TangemColors2.Graphic(
        neutral = TangemColors2.Graphic.Neutral(
            primary = TangemColorPalette.White,
            primaryInverted = TangemColorPalette.Dark6,
            secondary = TangemColorPalette.Light5,
            tertiary = TangemColorPalette.Dark3,
            quaternary = TangemColorPalette.Dark3,
            tertiaryConstant = TangemColorPalette.Dark3,
            primaryInvertedConstant = TangemColorPalette.White,
        ),
        status = TangemColors2.Graphic.Status(
            accent = TangemColorPalette.Azure,
            warning = TangemColorPalette.Flamingo,
            attention = TangemColorPalette.Mustard,
        ),
    )
    val border = TangemColors2.Border(
        neutral = TangemColors2.Border.Neutral(
            primary = TangemColorPalette.Dark4,
            secondary = TangemColorPalette.Dark4,
        ),
        status = TangemColors2.Border.Status(
            accent = TangemColorPalette.Azure,
            warning = TangemColorPalette.Flamingo,
            attention = TangemColorPalette.Mustard,
        ),
    )
    val overlay = TangemColors2.Overlay(
        overlayPrimary = TangemColorPalette.Overlay1,
        overlaySecondary = TangemColorPalette.Overlay2,
    )
    val fill = TangemColors2.Fill(
        neutral = TangemColors2.Fill.Neutral(
            primary = TangemColorPalette.White,
            primaryInverted = TangemColorPalette.Dark6,
            primaryInvertedConstant = TangemColorPalette.White,
            secondary = TangemColorPalette.Light5,
            tertiaryConstant = TangemColorPalette.Dark1,
            quaternary = TangemColorPalette.Dark3,
        ),
        status = TangemColors2.Fill.Status(
            accent = TangemColorPalette.Azure,
            warning = TangemColorPalette.Flamingo,
            attention = TangemColorPalette.Mustard,
        ),
    )
    val button = TangemColors2.Button(
        backgroundPrimary = TangemColorPalette.Light1V2,
        backgroundSecondary = TangemColorPalette.Light_10,
        backgroundDisabled = TangemColorPalette.Dark5,
        backgroundPositive = TangemColorPalette.Azure,
        backgroundPrimaryInverse = TangemColorPalette.Light_10,
        textSecondary = TangemColorPalette.Light4,
        textPrimary = TangemColorPalette.Dark4,
        textDisabled = text.neutral.secondary,
        iconPrimary = TangemColorPalette.Light4,
        iconSecondary = TangemColorPalette.Dark4,
        iconDisabled = TangemColorPalette.Dark5,
        borderPrimary = TangemColorPalette.Light4,
    )
    val surface = TangemColors2.Surface(
        level1 = TangemColorPalette.Dark6,
        level2 = TangemColorPalette.Black,
        level3 = TangemColorPalette.Dark6,
        level4 = TangemColorPalette.Dark5,
    )
    val controls = TangemColors2.Controls(
        backgroundChecked = TangemColorPalette.Azure,
        backgroundDefault = TangemColorPalette.Dark4,
        iconDefault = TangemColorPalette.White,
        iconDisabled = TangemColorPalette.White,
    )
    val field = TangemColors2.Field(
        backgroundDefault = TangemColorPalette.Dark6,
        backgroundFocused = TangemColorPalette.Dark4,
        textPlaceholder = text.neutral.secondary,
        textDefault = text.neutral.primary,
        textDisabled = text.neutral.tertiary,
        iconDefault = graphic.neutral.tertiary,
        iconDisabled = graphic.neutral.quaternary,
        textInvalid = text.status.warning,
        borderInvalid = border.status.warning,
    )
    val skeleton = TangemColors2.Skeleton(
        backgroundPrimary = TangemColorPalette.Dark5,
    )
    val markers = TangemColors2.Markers(
        backgroundSolidGray = TangemColorPalette.Dark5,
        backgroundDisabled = TangemColorPalette.Dark5,
        backgroundSolidBlue = TangemColorPalette.Azure,
        textGray = TangemColorPalette.Light4,
        textDisabled = text.neutral.secondary,
        iconGray = TangemColorPalette.Dark2,
        iconDisabled = TangemColorPalette.Dark5,
        borderGray = TangemColorPalette.White.copy(alpha = 0.2f),
        backgroundTintedBlue = TangemColorPalette.Azure.copy(alpha = 0.1f),
        textBlue = text.status.accent,
        backgroundSolidRed = TangemColorPalette.Amaranth,
        backgroundTintedRed = TangemColorPalette.Amaranth.copy(alpha = 0.1f),
        iconBlue = TangemColorPalette.Azure,
        iconRed = TangemColorPalette.Flamingo,
        textRed = TangemColorPalette.Flamingo,
        backgroundTintedGray = TangemColorPalette.White.copy(alpha = 0.1f),
        borderTintedBlue = TangemColorPalette.Azure.copy(alpha = 0.1f),
        borderTintedRed = TangemColorPalette.Amaranth.copy(alpha = 0.1f),
    )
    val tabs = TangemColors2.Tabs(
        textPrimary = TangemColorPalette.Dark4,
        textSecondary = TangemColorPalette.Light5,
        textTertiary = TangemColorPalette.White,
        backgroundPrimary = TangemColorPalette.Light1,
        backgroundSecondary = TangemColorPalette.Light_10,
        backgroundTertiary = TangemColorPalette.Light_10,
        backgroundQuaternary = TangemColorPalette.Light_10,
    )
    return TangemColors2(
        text = text,
        graphic = graphic,
        border = border,
        overlay = overlay,
        fill = fill,
        button = button,
        surface = surface,
        controls = controls,
        field = field,
        skeleton = skeleton,
        markers = markers,
        tabs = tabs,
    )
}