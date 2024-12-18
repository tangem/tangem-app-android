package com.tangem.tap.common.clipboard

import android.content.ClipData
import android.content.ClipDescription
import android.os.Build
import android.os.PersistableBundle
import com.tangem.core.ui.clipboard.ClipboardManager
import android.content.ClipboardManager as AndroidClipboardManager

internal class DefaultClipboardManager(private val clipboardManager: AndroidClipboardManager) : ClipboardManager {

    override fun setText(text: String, isSensitive: Boolean, label: String) {
        val clip = ClipData.newPlainText(label, text).apply {
            if (isSensitive) description.setAsSensitive()
        }

        clipboardManager.setPrimaryClip(clip)
    }

    private fun ClipDescription.setAsSensitive() {
        extras = PersistableBundle().apply {
            putBoolean(getExtraIsSensitiveFlag(), true)
        }
    }

    private fun getExtraIsSensitiveFlag(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ClipDescription.EXTRA_IS_SENSITIVE
        } else {
            "android.content.extra.IS_SENSITIVE"
        }
    }
}