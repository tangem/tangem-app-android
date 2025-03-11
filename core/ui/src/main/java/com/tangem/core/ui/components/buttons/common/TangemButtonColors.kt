package com.tangem.core.ui.components.buttons.common

import androidx.compose.material3.ButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Color

@Composable
fun ButtonColors.containerColor(enabled: Boolean): State<Color> {
    return rememberUpdatedState(newValue = if (enabled) containerColor else disabledContainerColor)
}

@Composable
fun ButtonColors.contentColor(enabled: Boolean): State<Color> {
    return rememberUpdatedState(newValue = if (enabled) contentColor else disabledContentColor)
}