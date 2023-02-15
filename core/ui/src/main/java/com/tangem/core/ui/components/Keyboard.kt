package com.tangem.core.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

sealed interface Keyboard {
    val height: Dp

    data class Opened(override val height: Dp) : Keyboard

    object Closed : Keyboard {
        override val height: Dp = 0.dp
    }
}

/**
 * Allows to subscribe to a soft keyboard to detect when it's open/closed
 */
@Deprecated("Use Modifier.imePadding() on pure Compose screens (without XML layouts)")
@Composable
fun keyboardAsState(): State<Keyboard> {
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
        .exclude(WindowInsets.navigationBars)
        .getBottom(density)

    return remember(imeInsets) {
        derivedStateOf {
            if (imeInsets > 0) {
                Keyboard.Opened(
                    height = with(density) { imeInsets.toDp() },
                )
            } else {
                Keyboard.Closed
            }
        }
    }
}
