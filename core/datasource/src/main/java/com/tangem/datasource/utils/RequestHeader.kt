package com.tangem.datasource.utils

import com.tangem.datasource.api.common.AuthProvider
import com.tangem.utils.Provider
import com.tangem.utils.version.AppVersionProvider

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
    )
}
