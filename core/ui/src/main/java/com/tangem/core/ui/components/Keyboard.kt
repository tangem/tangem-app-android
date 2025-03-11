package com.tangem.core.ui.components

import android.os.Build
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
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
    val bottomDp = with(LocalDensity.current) { bottom.toDp() }

    val keyboardStateInternal by rememberUpdatedState(
        if (bottom > 0) Keyboard.Opened(bottomDp) else Keyboard.Closed,
    )

    val keyboardState = remember { mutableStateOf(keyboardStateInternal) }

    LaunchedEffect(keyboardStateInternal) {
        val falsePositive = Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q &&
            keyboardStateInternal is Keyboard.Opened &&
            keyboardStateInternal.height < 50.dp
        // FIX android <=10 devices can randomly send ime paddings,
        // which leads to a false positive keyboard opening ([REDACTED_TASK_KEY])
        if (falsePositive) return@LaunchedEffect

        keyboardState.value = keyboardStateInternal
    }

    return keyboardState
}

@Composable
fun rememberIsKeyboardVisible(): State<Boolean> {
    val keyboard by keyboardAsState()
    return remember(keyboard) {
        derivedStateOf {
            keyboard.isOpened
        }
    }
}