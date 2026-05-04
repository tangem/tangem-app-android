package com.tangem.tap.common.clipboard

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipDescription.MIMETYPE_TEXT_HTML
import android.content.ClipDescription.MIMETYPE_TEXT_PLAIN
import android.os.Build
import android.os.PersistableBundle
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.utils.logging.TangemLogger
import android.content.ClipboardManager as AndroidClipboardManager

internal class DefaultClipboardManager(private val clipboardManager: AndroidClipboardManager) : ClipboardManager {

    override fun setText(text: String, isSensitive: Boolean, label: String) {
        val clip = ClipData.newPlainText(label, text).apply {
            if (isSensitive) description.setAsSensitive()
        }

        clipboardManager.setPrimaryClip(clip)
    }

    @Suppress("UseIsNullOrEmpty")
    override fun getText(default: String?): String? {
        val clip = clipboardManager.primaryClip

        if (clip == null || clip.itemCount == 0) {
            TangemLogger.d("Clipboard is empty")
            return default
        }

        val clipDescription = clipboardManager.primaryClipDescription
        if (clipDescription?.hasMimeType(MIMETYPE_TEXT_PLAIN) == false &&
            !clipDescription.hasMimeType(MIMETYPE_TEXT_HTML)
        ) {
            TangemLogger.d("Clipboard doesn't contain text")
            return default
        }

        return clip.getItemAt(0).text?.toString()
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