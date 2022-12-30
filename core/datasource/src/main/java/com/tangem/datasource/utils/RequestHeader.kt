package com.tangem.datasource.utils

import com.tangem.lib.auth.AuthProvider

/**
 * Presentation of request header
 *
 * @param pairs header name and header value pairs
 */
sealed class RequestHeader(vararg pairs: Pair<String, String>) {

    /** Header list */
    val values: List<Pair<String, String>> = pairs.toList()

    object CacheControlHeader : RequestHeader("Cache-Control" to "max-age=600")

    class AuthenticationHeader(authProvider: AuthProvider) : RequestHeader(
        "card_public_key" to authProvider.getCardPublicKey(),
        "card_id" to authProvider.getCardId()
    )
}
