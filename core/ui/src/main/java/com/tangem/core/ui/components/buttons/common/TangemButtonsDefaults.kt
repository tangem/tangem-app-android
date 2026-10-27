package com.tangem.core.ui.components.buttons.common

import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme

object TangemButtonsDefaults {

    val elevation: ButtonElevation
        @Composable
        get() = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
        )

    val primaryButtonColors: ButtonColors
        @Composable
        @ReadOnlyComposable
        get() = ButtonColors(
            containerColor = TangemTheme.colors3.bg.inverse,
            contentColor = TangemTheme.colors3.text.inverse.primary,
            disabledContainerColor = TangemTheme.colors3.bg.disabled,
            disabledContentColor = TangemTheme.colors3.text.tertiary,
        )

    val secondaryButtonColors: ButtonColors
        @Composable
        @ReadOnlyComposable
        get() = ButtonColors(
            containerColor = TangemTheme.colors3.bg.transparent.secondary,
            contentColor = TangemTheme.colors3.text.primary,
            disabledContainerColor = TangemTheme.colors3.bg.disabled,
            disabledContentColor = TangemTheme.colors3.text.tertiary,
        )

    val defaultTextButtonColors: ButtonColors
        @Composable
        @ReadOnlyComposable
        get() = ButtonColors(
            containerColor = Color.Transparent,
            contentColor = TangemTheme.colors3.text.secondary,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = TangemTheme.colors3.text.tertiary,
        )

    val warningTextButtonColors: ButtonColors
        @Composable
        @ReadOnlyComposable
        get() = ButtonColors(
            containerColor = Color.Transparent,
            contentColor = TangemTheme.colors3.text.accent.red,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = TangemTheme.colors3.text.tertiary,
        )

    val selectorButtonColors: ButtonColors
        @Composable
        @ReadOnlyComposable
        get() = ButtonColors(
            containerColor = Color.Transparent,
            contentColor = TangemTheme.colors3.text.secondary,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = TangemTheme.colors3.text.tertiary,
        )

    val positiveButtonColors: ButtonColors
        @Composable
        @ReadOnlyComposable
        get() = ButtonColors(
            containerColor = Color.Transparent,
            contentColor = TangemTheme.colors3.text.accent.blue,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = TangemTheme.colors3.text.tertiary,
        )
}