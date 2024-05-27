package com.tangem.tap.common.clipboard

import com.tangem.core.ui.clipboard.ClipboardManager
import timber.log.Timber

internal object MockClipboardManager : ClipboardManager {
    override fun setText(label: String, text: String) {
        /** Intentionnaly do nothing */
        Timber.w("Clipboard Manager not available")
    }
}