package com.tangem.core.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController

/**
 * [NestedScrollConnection] that calls [SoftwareKeyboardController.hide] on pre scroll
 *
 * @property keyboardController keyboard controller
 *
[REDACTED_AUTHOR]
 */
private class HideKeyboardNestedScrollConnection(
    private val keyboardController: SoftwareKeyboardController?,
) : NestedScrollConnection {

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        keyboardController?.hide()

        return super.onPreScroll(available, source)
    }
}

@Composable
fun rememberHideKeyboardNestedScrollConnection(): NestedScrollConnection {
    val keyboardController = LocalSoftwareKeyboardController.current
    return remember { HideKeyboardNestedScrollConnection(keyboardController = keyboardController) }
}