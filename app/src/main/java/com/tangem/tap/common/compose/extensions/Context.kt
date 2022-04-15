package com.tangem.tap.common.compose.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.tangem.tap.common.extensions.copyToClipboard
import com.tangem.tap.common.extensions.getFromClipboard

/**
[REDACTED_AUTHOR]
 */
@Composable
fun copyToClipboard(value: Any, label: String = "") {
    LocalContext.current.copyToClipboard(value, label)
}

@Composable
fun getFromClipboard(default: CharSequence? = null): CharSequence? {
    return LocalContext.current.getFromClipboard(default)
}