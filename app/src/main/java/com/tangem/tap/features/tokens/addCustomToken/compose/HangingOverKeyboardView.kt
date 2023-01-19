package com.tangem.tap.features.tokens.addCustomToken.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.Keyboard
import com.tangem.tangem_sdk_new.extensions.pxToDp

/**
 * Created by Anton Zhilenkov on 08/04/2022.
 */
@Composable
fun HangingOverKeyboardView(
    modifier: Modifier = Modifier,
    keyboardState: State<Keyboard>,
    spaceBetweenKeyboard: Dp = 0.dp,
    content: @Composable (BoxScope.() -> Unit)
) {
    val context = LocalContext.current
    val padding = when (keyboardState.value) {
        Keyboard.Closed -> 0.dp
        is Keyboard.Opened -> {
            val keyboardHeight = (keyboardState.value as Keyboard.Opened).height
            val keyboardPadding = context.pxToDp(keyboardHeight.toFloat()).dp
            keyboardPadding + spaceBetweenKeyboard
        }
    }

    Box(modifier.padding(bottom = padding)) { content() }
}
