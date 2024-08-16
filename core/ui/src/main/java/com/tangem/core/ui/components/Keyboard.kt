package com.tangem.core.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

sealed interface Keyboard {
    val height: Dp

    data class Opened(override val height: Dp) : Keyboard

    data object Closed : Keyboard {
        override val height: Dp = 0.dp
    }
}

val Keyboard.isOpened: Boolean
    @Stable
    get() = this is Keyboard.Opened

/**
 * Allows to subscribe to a soft keyboard to detect when it's open/closed
 */
@Composable
fun keyboardAsState(): State<Keyboard> {
    val bottom = WindowInsets.ime.getBottom(LocalDensity.current)
    val isImeVisible = bottom > 0
    return rememberUpdatedState(if (isImeVisible) Keyboard.Opened(bottom.dp) else Keyboard.Closed)
}