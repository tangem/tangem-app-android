package com.tangem.core.ui.components.buttons.common

import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.tangem.core.ui.res.TangemTheme

object TangemButtonsDefaults {

    val elevation: ButtonElevation
        @Composable
        get() = ButtonDefaults.buttonElevation(
            defaultElevation = TangemTheme.dimens.elevation0,
            pressedElevation = TangemTheme.dimens.elevation0,
        )

    val primaryButtonColors: ButtonColors
        @Composable
        @ReadOnlyComposable
        get() = ButtonColors(
            containerColor = TangemTheme.colors.button.primary,
            contentColor = TangemTheme.colors.text.primary2,
            disabledContainerColor = TangemTheme.colors.button.disabled,
            disabledContentColor = TangemTheme.colors.text.disabled,
        )

    val secondaryButtonColors: ButtonColors
        @Composable
        @ReadOnlyComposable
        get() = ButtonColors(
            containerColor = TangemTheme.colors.button.secondary,
            contentColor = TangemTheme.colors.text.primary1,
            disabledContainerColor = TangemTheme.colors.button.disabled,
            disabledContentColor = TangemTheme.colors.text.disabled,
        )

    val defaultTextButtonColors: ButtonColors
        @Composable
        @ReadOnlyComposable
        get() = ButtonColors(
            containerColor = Color.Transparent,
            contentColor = TangemTheme.colors.text.secondary,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = TangemTheme.colors.text.disabled,
        )

    val warningTextButtonColors: ButtonColors
        @Composable
        @ReadOnlyComposable
        get() = ButtonColors(
            containerColor = Color.Transparent,
            contentColor = TangemTheme.colors.text.warning,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = TangemTheme.colors.text.disabled,
        )

    val selectorButtonColors: ButtonColors
        @Composable
        @ReadOnlyComposable
        get() = ButtonColors(
            containerColor = Color.Transparent,
            contentColor = TangemTheme.colors.text.tertiary,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = TangemTheme.colors.text.disabled,
        )

    val positiveButtonColors: ButtonColors
        @Composable
        @ReadOnlyComposable
        get() = ButtonColors(
            containerColor = Color.Transparent,
            contentColor = TangemTheme.colors.text.accent,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = TangemTheme.colors.text.disabled,
        )
}