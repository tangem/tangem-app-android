package com.tangem.datasource.utils

import com.tangem.lib.auth.AuthProvider
import com.tangem.lib.auth.LazyAuthProvider

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
        "card_id" to authProvider.getCardId(),
        "card_public_key" to authProvider.getCardPublicKey(),
    )
}

sealed class LazyRequestHeader(vararg pairs: Pair<String, () -> String>) {

    val values: List<Pair<String, () -> String>> = pairs.toList()

    class LazyAuthenticationHeader(authProvider: LazyAuthProvider) : LazyRequestHeader(
        "card_id" to authProvider.getCardIdProvider(),
        "card_public_key" to authProvider.getCardPublicKeyProvider(),
    )
}