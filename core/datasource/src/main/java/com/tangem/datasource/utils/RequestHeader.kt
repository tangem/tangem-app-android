package com.tangem.datasource.utils

import com.tangem.datasource.api.common.AuthProvider
import com.tangem.lib.auth.AppVersionProvider
import com.tangem.lib.auth.ExpressAuthProvider

/**
 * Presentation of request header
 *
 * @param pairs header name and header value pairs
 */
sealed class RequestHeader(vararg pairs: Pair<String, () -> String>) {

    /** Header list */
    val values: List<Pair<String, () -> String>> = pairs.toList()

    data object CacheControlHeader : RequestHeader("Cache-Control" to { "max-age=600" })

    class AuthenticationHeader(authProvider: AuthProvider) : RequestHeader(
        "card_id" to { authProvider.getCardId() },
        "card_public_key" to { authProvider.getCardPublicKey() },
    )

    class Express(expressAuthProvider: ExpressAuthProvider) : RequestHeader(
        "api-key" to { expressAuthProvider.getApiKey() },
        "user-id" to { expressAuthProvider.getUserId() },
        "session-id" to { expressAuthProvider.getSessionId() },
    )

    class AppVersionPlatformHeaders(appVersionProvider: AppVersionProvider) : RequestHeader(
        "version" to { appVersionProvider.getAppVersion() },
        "platform" to { "android" },
    )
}
