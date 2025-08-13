package com.tangem.data.tokens.utils

import arrow.atomic.AtomicBoolean
import com.tangem.data.common.api.safeApiCall
import com.tangem.data.common.currency.UserTokensSaver
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.CoinsResponse
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Responsible for merging custom tokens into a user's token response.
 * It handles the logic to update tokens with additional details if necessary.
 *
 * @property tangemTechApi   Tangem Tech API
 * @property userTokensSaver user tokens saver
 * @property dispatchers     dispatchers
 */
internal class CustomTokensMerger(
    private val tangemTechApi: TangemTechApi,
    private val userTokensSaver: UserTokensSaver,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    /**
     * Merges custom tokens into the user's token response if needed.
     *
     * This function processes each token in the response asynchronously, checking if an update
     * is needed, and if so, updating the token from [TangemTechApi.getCoins] response. It then pushes to the backend
     * and returns an updated UserTokensResponse.
     *
     * @param userWalletId the identifier for the user's wallet, used when pushing updates
     * @param response     the original user tokens response that may need to be updated
     *
     * @return a potentially updated UserTokensResponse, with custom tokens merged if necessary
     */
    suspend fun mergeIfPresented(userWalletId: UserWalletId, response: UserTokensResponse): UserTokensResponse {
        // use flag to check: we can't compare two token list after merge because Token equals don't include some fields
        val wasMerged = AtomicBoolean(false)

        val mergedTokens = mergeTokens(response = response, wasMerged = wasMerged)

        val updatedResponse = response.copy(tokens = mergedTokens)

        // previously here was used compare response.tokens, but it's not working correctly
        // because Token.equals() skip some fields
        if (wasMerged.value) {
            userTokensSaver.push(userWalletId, updatedResponse)
        }

        return updatedResponse
    }

    private suspend fun mergeTokens(
        response: UserTokensResponse,
        wasMerged: AtomicBoolean,
    ): List<UserTokensResponse.Token> {
        return withContext(dispatchers.default) {
            response.tokens
                .map { token ->
                    async {
                        if (isCoinOrNonCustomToken(token)) return@async token

                        mergeToken(customToken = token, wasMerged = wasMerged)
                    }
                }
                .awaitAll()
        }
    }

    private fun isCoinOrNonCustomToken(token: UserTokensResponse.Token): Boolean {
        return token.contractAddress.isNullOrEmpty() || token.id != null
    }

    private suspend fun mergeToken(
        customToken: UserTokensResponse.Token,
        wasMerged: AtomicBoolean,
    ): UserTokensResponse.Token {
        val foundToken = findToken(token = customToken)

        return if (foundToken != null) {
            wasMerged.set(true)
            customToken.mergeWith(foundToken)
        } else {
            customToken
        }
    }

    /**
     * Find a [token] among the cryptocurrencies available for application.
     * If result is not null, then the token is available and should not be custom.
     */
    private suspend fun findToken(token: UserTokensResponse.Token): CoinsResponse.Coin? {
        return withContext(dispatchers.io) {
            val response = safeApiCall(
                call = {
                    tangemTechApi.getCoins(
                        contractAddress = token.contractAddress,
                        networkIds = token.networkId,
                    ).bind()
                },
                onError = {
                    Timber.e(it, "Unable to fetch token:\n$token")
                    null
                },
            )

            response?.coins?.firstOrNull()
        }
    }

    private fun UserTokensResponse.Token.mergeWith(coin: CoinsResponse.Coin): UserTokensResponse.Token {
        return copy(
            id = coin.id,
            name = coin.name,
            symbol = coin.symbol.ifEmpty { symbol },
        )
    }
}