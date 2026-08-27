package com.tangem.common.uri

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.tangem.utils.logging.TangemLogger
import java.net.URI

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

    /**
     * Check if [host] is a trusted Tangem host.
     *
     * Prefer this over [isUriTrusted] when the URI is already parsed: it neither re-parses the string nor reports
     * a malformed one to Crashlytics, which matters for externally supplied values such as a push payload.
     */
    fun isHostTrusted(host: String?): Boolean = host?.lowercase() in trustedHosts

    /** Check if [externalUri] is trusted */
    fun isUriTrusted(externalUri: String): Boolean {
        return try {
            val uri = URI.create(externalUri)

            uri.scheme == "https" && isHostTrusted(uri.host)
        } catch (e: Exception) {
            val exception = IllegalStateException("Failed to validate URI: $externalUri", e)

            TangemLogger.e("Error", exception)
            FirebaseCrashlytics.getInstance().recordException(exception)

            false
        }
    }
}