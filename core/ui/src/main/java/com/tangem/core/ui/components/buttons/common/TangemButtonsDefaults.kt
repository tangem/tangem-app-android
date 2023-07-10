package com.tangem.core.ui.components.buttons.common

import androidx.compose.material.ButtonColors
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ButtonElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.tangem.core.ui.res.TangemTheme

internal object TangemButtonsDefaults {

    val elevation: ButtonElevation
        @Composable get() = ButtonDefaults.elevation(
            defaultElevation = TangemTheme.dimens.elevation0,
            pressedElevation = TangemTheme.dimens.elevation0,
        )

    val primaryButtonColors: ButtonColors
        @Composable
        @ReadOnlyComposable
        get() = TangemButtonColors(
            backgroundColor = TangemTheme.colors.button.primary,
            contentColor = TangemTheme.colors.text.primary2,
            disabledBackgroundColor = TangemTheme.colors.button.disabled,
            disabledContentColor = TangemTheme.colors.text.disabled,
        )

    val secondaryButtonColors: ButtonColors
        @Composable
        @ReadOnlyComposable
        get() = TangemButtonColors(
            backgroundColor = TangemTheme.colors.button.secondary,
            contentColor = TangemTheme.colors.text.primary1,
            disabledBackgroundColor = TangemTheme.colors.button.disabled,
            disabledContentColor = TangemTheme.colors.text.disabled,
        )

    val defaultTextButtonColors: ButtonColors
        @Composable
        @ReadOnlyComposable
        get() = TangemButtonColors(
            backgroundColor = Color.Transparent,
            contentColor = TangemTheme.colors.text.secondary,
            disabledBackgroundColor = Color.Transparent,
            disabledContentColor = TangemTheme.colors.text.disabled,
        )

    val warningTextButtonColors: ButtonColors
        @Composable
        @ReadOnlyComposable
        get() = TangemButtonColors(
            backgroundColor = Color.Transparent,
            contentColor = TangemTheme.colors.text.warning,
            disabledBackgroundColor = Color.Transparent,
            disabledContentColor = TangemTheme.colors.text.disabled,
        )

    val selectorButtonColors: ButtonColors
        @Composable
        @ReadOnlyComposable
        get() = TangemButtonColors(
            backgroundColor = Color.Transparent,
            contentColor = TangemTheme.colors.text.tertiary,
            disabledBackgroundColor = Color.Transparent,
            disabledContentColor = TangemTheme.colors.text.disabled,
        )

    val backgroundButtonColors: ButtonColors
        @Composable
        @ReadOnlyComposable
        get() = TangemButtonColors(
            backgroundColor = TangemTheme.colors.background.primary,
            contentColor = TangemTheme.colors.text.primary1,
            disabledBackgroundColor = TangemTheme.colors.button.disabled,
            disabledContentColor = TangemTheme.colors.text.disabled,
        )
}
