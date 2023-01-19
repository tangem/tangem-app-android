package com.tangem.core.ui.components

import android.graphics.Rect
import android.view.ViewTreeObserver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

sealed class Keyboard {
    data class Opened(val height: Int) : Keyboard()
    object Closed : Keyboard()
}

/**
 * Allows to subscribe to a soft keyboard to detect when it's open/closed
 */
@Suppress("MagicNumber")
@Composable
fun keyboardAsState(): State<Keyboard> {
    val keyboardState: MutableState<Keyboard> = remember { mutableStateOf(Keyboard.Closed) }
    val view = LocalView.current
    val discrepancy = remember {
        mutableStateOf(0)
    }
    DisposableEffect(view) {
        val onGlobalListener = ViewTreeObserver.OnGlobalLayoutListener {
            val rect = Rect()
            view.getWindowVisibleDisplayFrame(rect)
            val screenHeight = view.rootView.height
            val keypadHeight: Int = screenHeight - (rect.bottom + rect.top) - discrepancy.value
            if (discrepancy.value == 0) {
                discrepancy.value = keypadHeight
                if (keypadHeight == 0) discrepancy.value = 1
            }

            keyboardState.value = if (keypadHeight > screenHeight * 0.15) {
                Keyboard.Opened(keypadHeight)
            } else {
                Keyboard.Closed
            }
        }
        view.viewTreeObserver.addOnGlobalLayoutListener(onGlobalListener)

        onDispose {
            view.viewTreeObserver.removeOnGlobalLayoutListener(onGlobalListener)
        }
    }

    return keyboardState
}
