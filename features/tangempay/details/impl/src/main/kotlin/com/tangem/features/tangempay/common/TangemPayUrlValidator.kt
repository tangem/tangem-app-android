package com.tangem.features.tangempay.common

import java.net.URI

internal object TangemPayUrlValidator {

    private const val HTTPS_SCHEME = "https"

    private val trustedLinkHosts: Set<String> = setOf(
        "tangem.com",
        "www.tangem.com",
        "paera.com",
        "www.paera.com",
    )

    fun isLinkTrusted(url: String): Boolean {
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        return uri.scheme == HTTPS_SCHEME && uri.userInfo == null && uri.host?.lowercase() in trustedLinkHosts
    }

    fun isImageUrlSecure(url: String): Boolean = url.startsWith("$HTTPS_SCHEME://")
}