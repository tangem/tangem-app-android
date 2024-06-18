package com.tangem.tap.common.clipboard

import android.content.ClipData
import com.tangem.core.ui.clipboard.ClipboardManager
import android.content.ClipboardManager as AndroidClipboardManager

internal class DefaultClipboardManager(private val clipboardManager: AndroidClipboardManager) : ClipboardManager {

    override fun setText(label: String, text: String) {
        clipboardManager.setPrimaryClip(ClipData.newPlainText(label, text))
    }
}