package com.tangem.datasource.utils

import android.os.Build
import com.tangem.datasource.api.common.AuthProvider
import com.tangem.datasource.utils.RequestHeader.CacheControlHeader.checkHeaderValueOrEmpty
import com.tangem.utils.Provider
import com.tangem.utils.version.AppVersionProvider
import java.util.Locale
import java.util.TimeZone

/**
 * Presentation of request header
 *
 * @param pairs header name and header value pairs
 */
sealed class RequestHeader(vararg pairs: Pair<String, Provider<String>>) {

    /** Header list */
    val values: Map<String, Provider<String>> = pairs.toMap()

    data object CacheControlHeader : RequestHeader("Cache-Control" to Provider { "max-age=600" })

    class AuthenticationHeader(authProvider: AuthProvider) : RequestHeader(
        "card_id" to Provider(authProvider::getCardId),
        "card_public_key" to Provider(authProvider::getCardPublicKey),
    )

    class AppVersionPlatformHeaders(appVersionProvider: AppVersionProvider) : RequestHeader(
        "version" to Provider(appVersionProvider::versionName),
        "platform" to Provider { "android" },
        "language" to Provider { Locale.getDefault().language.checkHeaderValueOrEmpty() },
        "timezone" to Provider {
            TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT).checkHeaderValueOrEmpty()
        },
        "device" to Provider { "${Build.MANUFACTURER} ${Build.MODEL}".checkHeaderValueOrEmpty() },
    )

    /**
     * Use it to avoid crash in okhttp headers
     */
    fun String.checkHeaderValueOrEmpty(): String {
        for (i in this.indices) {
            val c = this[i]
            val charCondition = c == '\t' || c in '\u0020'..'\u007e'
            if (!charCondition) {
                return ""
            }
        }
        return this
    }
}