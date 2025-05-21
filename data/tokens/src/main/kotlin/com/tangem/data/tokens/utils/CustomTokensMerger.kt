package com.tangem.data.tokens.utils

import arrow.atomic.AtomicBoolean
import com.tangem.data.common.api.safeApiCall
import com.tangem.data.common.currency.UserTokensSaver
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Responsible for merging custom tokens into a user's token response.
 * It handles the logic to update tokens with additional details if necessary.
 */
internal class CustomTokensMerger(
    private val tangemTechApi: TangemTechApi,
    private val dispatchers: CoroutineDispatcherProvider,
    private val userTokensSaver: UserTokensSaver,
) {

    /**
     * Merges custom tokens into the user's token response if needed.
     *
     * This function processes each token in the response asynchronously, checking if an update
     * is needed, and if so, updating the token from [TangemTechApi.getCoins] response. It then pushes to the backend
     * and returns an updated UserTokensResponse.
     *
     * @param userWalletId The identifier for the user's wallet, used when pushing updates.
     * @param response The original user tokens response that may need to be updated.
     * @return A potentially updated UserTokensResponse, with custom tokens merged if necessary.
     */
    suspend fun mergeIfPresented(userWalletId: UserWalletId, response: UserTokensResponse): UserTokensResponse {
        // use flag to check: we can't compare two token list after merge because Token equals don't include some fields
        val wasMerged = AtomicBoolean(false)

        val mergedTokens = withContext(dispatchers.default) {
            response.tokens
                .map { token ->
                    async { mergeIfPresented(token, wasMerged) }
                }
                .awaitAll()
        }
        val updatedResponse = response.copy(tokens = mergedTokens)

        // previously here was used compare response.tokens, but it's not working correctly
        // because Token.equals() skip some fields
        if (wasMerged.value) {
            userTokensSaver.push(userWalletId, updatedResponse)
        }

        return updatedResponse
    }

    private suspend fun mergeIfPresented(
        token: UserTokensResponse.Token,
        wasMerged: AtomicBoolean,
    ): UserTokensResponse.Token {
        if (isCoinOrNonCustomToken(token)) return token

        return merge(token, wasMerged)
    }

    private suspend fun merge(
        customToken: UserTokensResponse.Token,
        wasMerged: AtomicBoolean,
    ): UserTokensResponse.Token {
        val foundToken = fetchToken(customToken)
        if (foundToken != null) {
            wasMerged.set(true)
        }
        return foundToken ?: customToken
    }

    private fun isCoinOrNonCustomToken(token: UserTokensResponse.Token): Boolean {
        return token.contractAddress.isNullOrEmpty() || token.id != null
    }

    private suspend fun fetchToken(token: UserTokensResponse.Token): UserTokensResponse.Token? {
        val response = withContext(dispatchers.io) {
            safeApiCall(
                call = {
                    tangemTechApi.getCoins(
                        contractAddress = token.contractAddress,
                        networkIds = token.networkId,
                    ).bind()
                },
                onError = {
                    Timber.w(it, "Unable to fetch token")
                    null
                },
            )
        }
        val foundToken = response?.coins?.firstOrNull() ?: return null

        return token.copy(
            id = foundToken.id,
            name = foundToken.name,
            symbol = foundToken.symbol.ifEmpty {
                token.symbol
            },
        )
    }
}