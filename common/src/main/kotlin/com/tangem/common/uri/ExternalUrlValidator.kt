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

    /** Check if [externalUri] is trusted */
    fun isUriTrusted(externalUri: String): Boolean {
        return try {
            val uri = URI.create(externalUri)

            uri.scheme == "https" && uri.host in trustedHosts
        } catch (e: Exception) {
            val exception = IllegalStateException("Failed to validate URI: $externalUri", e)

            TangemLogger.e("Error", exception)
            FirebaseCrashlytics.getInstance().recordException(exception)

            false
        }
    }
}