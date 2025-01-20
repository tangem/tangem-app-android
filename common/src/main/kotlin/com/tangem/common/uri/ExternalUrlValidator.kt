package com.tangem.common.uri

import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber
import java.net.URI

/**
 * External url validator
 *
[REDACTED_AUTHOR]
 */
object ExternalUrlValidator {

    private val trustedHost: List<String> = listOf("tangem.com")

    /** Check if [externalUri] is trusted */
    fun isUriTrusted(externalUri: String): Boolean {
        return try {
            val uri = URI.create(externalUri)

            uri.scheme == "https" && uri.host in trustedHost
        } catch (e: Exception) {
            val exception = IllegalStateException("Failed to validate URI: $externalUri", e)

            Timber.e(exception)
            FirebaseCrashlytics.getInstance().recordException(exception)

            false
        }
    }
}