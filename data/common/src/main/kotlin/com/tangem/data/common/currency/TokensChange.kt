package com.tangem.data.common.currency

import com.tangem.datasource.api.tangemTech.models.UserTokensResponse

/**
 * What the user did to the token list, as opposed to the list it resulted in. A stale write is recovered by applying
 * it to the list the backend has now: the two lists alone cannot tell a token the user removed from one another
 * member added.
 */
data class TokensChange(
    val added: List<UserTokensResponse.Token>,
    val removed: List<UserTokensResponse.Token>,
) {

    fun applyTo(tokens: List<UserTokensResponse.Token>): List<UserTokensResponse.Token> {
        val removedTokens = removed.toSet()
        val keptTokens = tokens.filterNot { it in removedTokens }
        val keptTokenSet = keptTokens.toSet()

        return keptTokens + added.filterNot { it in keptTokenSet }
    }
}