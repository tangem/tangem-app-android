package com.tangem.common.uri

import android.net.Uri

/**
 * External url validator
 *
[REDACTED_AUTHOR]
 */
object ExternalUrlValidator {

    private val trustedHosts: Set<String> = setOf(
        "tangem.com",
        "www.tangem.com",
        "buy.tangem.com",
        "app.tangem.com",
        "tangem.surveysparrow.com",
        "feedback.tangem.com",
    )

    /** Check if [host] is a trusted Tangem host */
    fun isHostTrusted(host: String?): Boolean = host?.lowercase() in trustedHosts

    /**
     * Check if [uri] is a trusted https link.
     *
     * User info is rejected because Tangem links never carry it, while parsers disagree on where it ends: an unpatched
     * [Uri] reads `https://evil.com\@tangem.com` as host `tangem.com`, a browser as `evil.com`.
     */
    fun isUriTrusted(uri: Uri): Boolean = uri.scheme == "https" && uri.userInfo == null && isHostTrusted(uri.host)
}