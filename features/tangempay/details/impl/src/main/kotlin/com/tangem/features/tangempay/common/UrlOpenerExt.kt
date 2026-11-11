package com.tangem.features.tangempay.common

import com.tangem.core.navigation.url.UrlOpener
import com.tangem.utils.logging.TangemLogger

internal fun UrlOpener.openTrustedLink(url: String) {
    val trimmedUrl = url.trim()
    if (TangemPayUrlValidator.isLinkTrusted(trimmedUrl)) {
        openUrl(trimmedUrl)
    } else {
        TangemLogger.e("Untrusted Tangem Pay link is not opened: ${trimmedUrl.substringBefore('?')}")
    }
}