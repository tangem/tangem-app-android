package com.tangem.core.ui.clipboard

import androidx.compose.runtime.Stable

/**
 * Ready to use clipboard manager.
 * Does not require context to work
 */
@Stable
interface ClipboardManager {

    /**
     * Copies to clipboard [text] with optional [label]
     *
     * @param text        text
     * @param isSensitive flag that determines whether the text is sensitive
     * @param label       label
     */
    fun setText(text: String, isSensitive: Boolean, label: String = "")

    /**
     * Get text (text/plain) from clipboard
     *
     * @param default default value
     */
    fun getText(default: String? = null): String?
}