package com.tangem.tap.common.clipboard

import com.tangem.core.ui.clipboard.ClipboardManager
import timber.log.Timber

internal object MockClipboardManager : ClipboardManager {

    override fun setText(text: String, isSensitive: Boolean, label: String) {
        Timber.w("Clipboard Manager not available")
    }

    override fun getText(default: String?): String? = null
}