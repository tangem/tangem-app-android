package com.tangem.data.tokens.utils

import com.tangem.data.common.api.safeApiCall
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
        val mergedTokens = withContext(dispatchers.default) {
            response.tokens
                .map { token ->
                    async { mergeIfPresented(token) }
                }
                .awaitAll()
        }
        val updatedResponse = response.copy(tokens = mergedTokens)

        if (response.tokens != updatedResponse.tokens) {
            pushTokens(userWalletId, updatedResponse)
        }

        return updatedResponse
    }

    private suspend fun mergeIfPresented(token: UserTokensResponse.Token): UserTokensResponse.Token {
        if (isCoinOrNonCustomToken(token)) return token

        return merge(token)
    }

    private suspend fun merge(customToken: UserTokensResponse.Token): UserTokensResponse.Token {
        val foundToken = fetchToken(customToken)

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
            symbol = foundToken.symbol,
        )
    }

    private suspend fun pushTokens(userWalletId: UserWalletId, response: UserTokensResponse) {
        safeApiCall({ tangemTechApi.saveUserTokens(userWalletId.stringValue, response).bind() }) {
            Timber.e(it, "Unable to save user tokens for: $userWalletId")
        }
    }
}