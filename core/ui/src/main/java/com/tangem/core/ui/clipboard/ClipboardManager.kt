package com.tangem.core.ui.clipboard

import androidx.compose.runtime.Stable

/**
 * Ready to use clipboard manager.
 * Does not require context to work
 */
@Stable
interface ClipboardManager {

    /** Copies to clipboard [text] with optional [label] */
    fun setText(label: String = "", text: String)
}
