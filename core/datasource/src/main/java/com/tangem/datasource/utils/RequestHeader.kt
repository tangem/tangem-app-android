package com.tangem.datasource.utils

import com.tangem.datasource.api.common.AuthProvider
import com.tangem.datasource.api.common.config.ApiEnvironment
import com.tangem.datasource.utils.RequestHeader.CacheControlHeader.checkHeaderValueOrEmpty
import com.tangem.utils.Provider
import com.tangem.utils.ProviderSuspend
import com.tangem.utils.info.AppInfoProvider
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
        appInfoProvider: AppInfoProvider,
    ) : RequestHeader(
        "system_version" to ProviderSuspend { appInfoProvider.osVersion },
        "version" to ProviderSuspend { appInfoProvider.appVersion },
        "platform" to ProviderSuspend { "android" },
        "language" to ProviderSuspend { appInfoProvider.language.checkHeaderValueOrEmpty() },
        "timezone" to ProviderSuspend {
            TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT).checkHeaderValueOrEmpty()
        },
        "device" to ProviderSuspend { appInfoProvider.device.checkHeaderValueOrEmpty() },
    )

    /**
     * Use ONLY for tangemApi (not express or yields)
     */
    class TangemApiKeyHeader(authProvider: AuthProvider, apiEnvironment: Provider<ApiEnvironment>) : RequestHeader(
        "api-key" to authProvider.getApiKey(apiEnvironment),
    )

    /**
     * Use it to avoid crash in okhttp headers
     */
    fun String.checkHeaderValueOrEmpty(): String {
        for (i in this.indices) {
            val c = this[i]
            val isChar = c == '\t' || c in '\u0020'..'\u007e'
            if (!isChar) {
                return ""
            }
        }
        return this
    }
}