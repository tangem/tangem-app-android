package com.tangem.datasource.utils

import android.os.Build
import com.tangem.datasource.api.common.AuthProvider
import com.tangem.datasource.utils.RequestHeader.CacheControlHeader.checkHeaderValueOrEmpty
import com.tangem.utils.ProviderSuspend
import com.tangem.utils.info.AppInfoProvider
import com.tangem.utils.version.AppVersionProvider
import java.util.Locale
import java.util.TimeZone

/**
 * Presentation of request header
 *
 * @param pairs header name and header value pairs
 */
sealed class RequestHeader(vararg pairs: Pair<String, ProviderSuspend<String>>) {

    /** Header list */
    val values: Map<String, ProviderSuspend<String>> = pairs.toMap()

    data object CacheControlHeader : RequestHeader("Cache-Control" to ProviderSuspend { "max-age=600" })

    class AuthenticationHeader(authProvider: AuthProvider) : RequestHeader(
        "card_id" to ProviderSuspend(authProvider::getCardId),
        "card_public_key" to ProviderSuspend(authProvider::getCardPublicKey),
    )

    class AppVersionPlatformHeaders(
        appVersionProvider: AppVersionProvider,
        appInfoProvider: AppInfoProvider,
    ) : RequestHeader(
        "system_version" to ProviderSuspend { appInfoProvider.osVersion },
        "version" to ProviderSuspend { appVersionProvider.versionName },
        "platform" to ProviderSuspend { "android" },
        "language" to ProviderSuspend { Locale.getDefault().language.checkHeaderValueOrEmpty() },
        "timezone" to ProviderSuspend {
            TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT).checkHeaderValueOrEmpty()
        },
        "device" to ProviderSuspend { "${Build.MANUFACTURER} ${Build.MODEL}".checkHeaderValueOrEmpty() },
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