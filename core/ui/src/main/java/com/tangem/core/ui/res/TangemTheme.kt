package com.tangem.core.ui.res

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.tangem.core.ui.UiDependencies
import com.tangem.core.ui.components.TangemShimmer
import com.tangem.core.ui.components.text.BladeAnimation
import com.tangem.core.ui.components.text.rememberBladeAnimation
import com.tangem.core.ui.haptic.DefaultHapticManager
import com.tangem.core.ui.haptic.HapticManager
import com.tangem.core.ui.haptic.VibratorHapticManager
import com.tangem.core.ui.message.EventMessageHandler
import com.tangem.core.ui.windowsize.WindowSize
import com.tangem.core.ui.windowsize.rememberWindowSize
import com.tangem.domain.apptheme.model.AppThemeMode
import com.valentinilk.shimmer.Shimmer

@Composable
fun TangemTheme(
    activity: Activity,
    uiDependencies: UiDependencies,
    typography: TangemTypography = TangemTheme.typography,
    dimens: TangemDimens = TangemTheme.dimens,
    overrideSystemBarColors: Boolean = true,
    content: @Composable () -> Unit,
) {
    val appThemeMode by uiDependencies.appThemeModeHolder.appThemeMode
    val windowSize = rememberWindowSize(activity = activity)

    TangemTheme(
        isDark = shouldUseDarkTheme(appThemeMode),
        windowSize = windowSize,
        vibratorHapticManager = uiDependencies.vibratorHapticManager,
        snackbarHostState = uiDependencies.globalSnackbarHostState,
        eventMessageHandler = uiDependencies.eventMessageHandler,
        overrideSystemBarColors = overrideSystemBarColors,
        typography = typography,
        dimens = dimens,
        content = content,
    )
}

@Composable
fun TangemTheme(
    windowSize: WindowSize,
    typography: TangemTypography = TangemTheme.typography,
    dimens: TangemDimens = TangemTheme.dimens,
    isDark: Boolean = false,
    vibratorHapticManager: VibratorHapticManager? = null,
    eventMessageHandler: EventMessageHandler = remember { EventMessageHandler() },
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    overrideSystemBarColors: Boolean = true,
    content: @Composable () -> Unit,
) {
    val themeColors = if (isDark) darkThemeColors() else lightThemeColors()
    val rememberedColors = remember { themeColors }
        .also { it.update(themeColors) }

    val shapes = remember { TangemShapes(dimens) }

    // we don't want to override system bar colors in case of fragment bottom sheets for example
    if (overrideSystemBarColors) {
        val systemUiController = rememberSystemUiController()

        SideEffect {
            systemUiController.setSystemBarsColor(
                color = Color.Transparent,
                darkIcons = !isDark,
                isNavigationBarContrastEnforced = false,
            )
        }
    }

    val view = LocalView.current

    val hapticManager = remember(view) {
        DefaultHapticManager(view = view, vibratorHapticManager = vibratorHapticManager)
    }

    val rootBackgroundColor = rememberedColors.background.secondary

    MaterialTheme(
        colorScheme = tangemColorScheme(colors = themeColors),
    ) {
        CompositionLocalProvider(
            LocalTangemColors provides rememberedColors,
            LocalTangemTypography provides typography,
            LocalTangemDimens provides dimens,
            LocalTangemShapes provides shapes,
            LocalIsInDarkTheme provides isDark,
            LocalHapticManager provides hapticManager,
            LocalSnackbarHostState provides snackbarHostState,
            LocalEventMessageHandler provides eventMessageHandler,
            LocalWindowSize provides windowSize,
            LocalBladeAnimation provides rememberBladeAnimation(),
        ) {
            CompositionLocalProvider(
                LocalTangemShimmer provides TangemShimmer,
                LocalMainBottomSheetColor provides remember { mutableStateOf(Color.Unspecified) },
                LocalRootBackgroundColor provides remember(rootBackgroundColor) { mutableStateOf(rootBackgroundColor) },
                LocalTextSelectionColors provides TangemTextSelectionColors,
            ) {
                ProvideTextStyle(
                    value = TangemTheme.typography.body1,
                    content = content,
                )
            }
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
private fun tangemColorScheme(colors: TangemColors): ColorScheme {
    return ColorScheme(
        primary = colors.background.primary,
        onPrimary = colors.text.primary1,
        primaryContainer = colors.background.secondary,
        onPrimaryContainer = colors.background.action,
        inversePrimary = colors.background.action,

        secondary = colors.button.primary,
        onSecondary = colors.text.primary1,
        secondaryContainer = colors.background.secondary,
        onSecondaryContainer = colors.text.primary1,

        tertiary = colors.background.tertiary,
        onTertiary = colors.text.tertiary,
        tertiaryContainer = colors.background.tertiary,
        onTertiaryContainer = colors.text.tertiary,

        background = colors.background.primary,
        onBackground = colors.text.primary1,

        surface = colors.background.secondary,
        surfaceVariant = colors.background.tertiary,
        onSurface = colors.text.primary1,
        onSurfaceVariant = colors.text.secondary,
        surfaceTint = colors.background.tertiary,
        inverseSurface = colors.button.disabled,
        inverseOnSurface = colors.button.primary,
        surfaceBright = colors.background.secondary,
        surfaceDim = colors.background.tertiary,
        surfaceContainer = colors.background.tertiary,
        surfaceContainerHigh = colors.background.tertiary,
        surfaceContainerHighest = colors.background.tertiary,
        surfaceContainerLow = colors.background.tertiary,
        surfaceContainerLowest = colors.background.tertiary,

        error = colors.text.warning,
        errorContainer = colors.background.tertiary,
        onErrorContainer = colors.text.primary2,
        onError = colors.text.primary2,

        outline = colors.stroke.primary,
        outlineVariant = colors.stroke.secondary,

        scrim = colors.stroke.transparency,
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
            secondary = TangemColorPalette.Light5,
            transparency = TangemColorPalette.White,
        ),
        field = TangemColors.Field(
            primary = TangemColorPalette.Light1,
            focused = TangemColorPalette.Light2,
        ),
        overlay = TangemColors.Overlay(
            primary = TangemColorPalette.Black.copy(alpha = 0.4f),
            secondary = TangemColorPalette.Black.copy(alpha = 0.7f),
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
        overlay = TangemColors.Overlay(
            primary = TangemColorPalette.Black.copy(alpha = 0.4f),
            secondary = TangemColorPalette.Black.copy(alpha = 0.7f),
        ),
    )
}

private val TangemTextSelectionColors: TextSelectionColors
    @Composable
    @ReadOnlyComposable
    get() = TextSelectionColors(
        handleColor = TangemTheme.colors.text.accent,
        backgroundColor = TangemTheme.colors.text.accent.copy(alpha = 0.3f),
    )

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

val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState> {
    error("No SnackbarHostState provided")
}

val LocalEventMessageHandler = staticCompositionLocalOf<EventMessageHandler> {
    error("No EventMessageHandler provided")
}

val LocalWindowSize = staticCompositionLocalOf<WindowSize> {
    error("No WindowSize provided")
}

val LocalTangemShimmer = staticCompositionLocalOf<Shimmer> {
    error("No TangemShimmer provided")
}

val LocalMainBottomSheetColor = staticCompositionLocalOf<MutableState<Color>> {
    error("No MainBottomSheetColor provided")
}

val LocalRootBackgroundColor = staticCompositionLocalOf<MutableState<Color>> {
    error("No MainBottomSheetColor provided")
}

val LocalBladeAnimation = staticCompositionLocalOf<BladeAnimation> {
    error("No MainBottomSheetColor provided")
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