package com.tangem.core.ui.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.SoftwareKeyboardController

/**
 * [NestedScrollConnection] that calls [SoftwareKeyboardController.hide] on pre scroll
 *
 * @property keyboardController keyboard controller
 *
 * @author Andrew Khokhlov on 29/11/2024
 */
class HideKeyboardNestedScrollConnection(
    private val keyboardController: SoftwareKeyboardController?,
) : NestedScrollConnection {

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        keyboardController?.hide()

        return super.onPreScroll(available, source)
    }
}
