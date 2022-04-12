package com.tangem.tap.features.tokens.addCustomToken.compose

import android.content.Context
import android.util.TypedValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.tangem_sdk_new.extensions.pxToDp
import com.tangem.tap.common.compose.Keyboard

/**
[REDACTED_AUTHOR]
 */
@Composable
fun HangingOverKeyboardView(
    modifier: Modifier = Modifier,
    keyboardState: State<Keyboard>,
    defaultBottomPadding: Dp = 0.dp,
    spaceBetweenKeyboard: Dp = 10.dp,
    calculateWithActionBarHeight: Boolean = true,
    content: @Composable() (BoxScope.() -> Unit)
) {
    fun getActionBarHeight(context: Context): Int {
        val typedValue = TypedValue()
        return if (context.theme.resolveAttribute(android.R.attr.actionBarSize, typedValue, true)) {
            val data = typedValue.data
            val displayMetrics = context.resources.displayMetrics
            TypedValue.complexToDimensionPixelSize(data, displayMetrics)
        } else {
            0
        }
    }

    val context = LocalContext.current
    val calculatedPadding = when (keyboardState.value) {
        Keyboard.Closed -> defaultBottomPadding
        is Keyboard.Opened -> {
            val keyboardHeight = (keyboardState.value as Keyboard.Opened).height
            val keyboardPadding = context.pxToDp(keyboardHeight.toFloat()).dp
            if (calculateWithActionBarHeight) {
                val actionBarHeight = context.pxToDp(getActionBarHeight(context).toFloat()).dp
                keyboardPadding + spaceBetweenKeyboard - actionBarHeight
            } else {
                keyboardPadding + spaceBetweenKeyboard
            }

        }
    }
    Box(modifier.padding(bottom = calculatedPadding)) { content() }
}