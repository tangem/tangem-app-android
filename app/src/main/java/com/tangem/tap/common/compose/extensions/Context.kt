package com.tangem.tap.common.compose.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.tangem.tap.common.extensions.copyToClipboard
import com.tangem.tap.common.extensions.getFromClipboard

/**
 * Created by Anton Zhilenkov on 14/04/2022.
 */
@Suppress("ComposableFunctionName")
@Composable
fun copyToClipboard(value: Any, label: String = "") {
    LocalContext.current.copyToClipboard(value, label)
}

@Suppress("ComposableFunctionName")
@Composable
fun getFromClipboard(default: CharSequence? = null): CharSequence? {
    return LocalContext.current.getFromClipboard(default)
}
