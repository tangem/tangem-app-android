package com.tangem.tap.common.clipboard

import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.utils.logging.TangemLogger

internal object MockClipboardManager : ClipboardManager {

    override fun setText(text: String, isSensitive: Boolean, label: String) {
        TangemLogger.w("Clipboard Manager not available")
    }

    override fun getText(default: String?): String? = null
}