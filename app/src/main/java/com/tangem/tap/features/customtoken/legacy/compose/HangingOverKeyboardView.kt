package com.tangem.tap.features.customtoken.legacy.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.Keyboard

/**
 * Created by Anton Zhilenkov on 08/04/2022.
 */
@Composable
fun HangingOverKeyboardView(
    modifier: Modifier = Modifier,
    keyboardState: State<Keyboard>,
    spaceBetweenKeyboard: Dp = 0.dp,
    content: @Composable (BoxScope.() -> Unit),
) {
    val padding = remember(keyboardState) {
        when (val state = keyboardState.value) {
            is Keyboard.Closed -> 0.dp
            is Keyboard.Opened -> state.height + spaceBetweenKeyboard
        }
    }

    Box(modifier.padding(bottom = padding)) { content() }
}
