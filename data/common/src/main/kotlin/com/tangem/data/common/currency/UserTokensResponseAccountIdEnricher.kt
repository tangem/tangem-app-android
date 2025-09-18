package com.tangem.data.common.currency

import arrow.core.getOrElse
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.lib.crypto.derivation.AccountNodeRecognizer
import timber.log.Timber

/**
 * Enriches the [UserTokensResponse] with accountId values for tokens
 *
[REDACTED_AUTHOR]
 */
object UserTokensResponseAccountIdEnricher {

    /**
     * Enriches the tokens in the given [UserTokensResponse] with accountId values
     *
     * @param userWalletId the ID of the user wallet
     * @param response     the [UserTokensResponse] containing tokens to be enriched
     */
    operator fun invoke(userWalletId: UserWalletId, response: UserTokensResponse): UserTokensResponse {
        val enrichedTokens = invoke(userWalletId = userWalletId, tokens = response.tokens)

        return response.copy(tokens = enrichedTokens)
    }

    /**
     * Enriches the given list of tokens with accountId values
     *
     * @param userWalletId the ID of the user wallet
     * @param tokens       the list of tokens to be enriched
     */
    operator fun invoke(
        userWalletId: UserWalletId,
        tokens: List<UserTokensResponse.Token>,
    ): List<UserTokensResponse.Token> {
        val hasUnassignedTokens = tokens.any { it.accountId == null }
        if (!hasUnassignedTokens) return tokens

        val enrichedTokens = tokens
            .filter { it.accountId == null }
            .groupByAccountIndex()
            .mapKeysToAccountId(userWalletId)
            .mapToEnrichedTokens()

        if (enrichedTokens.isEmpty()) return tokens

        val enrichedTokenMap = enrichedTokens.associateBy { it }
        return tokens.map { token ->
            enrichedTokenMap[token] ?: token
        }
    }

    private fun List<UserTokensResponse.Token>.groupByAccountIndex(): Map<Long?, List<UserTokensResponse.Token>> {
        return this
            .groupBy { savedToken ->
                val derivationPathValue = savedToken.derivationPath
                if (derivationPathValue == null) {
                    Timber.e("Token $savedToken has no derivation path")
                    return@groupBy null
                }

                val blockchain = Blockchain.fromNetworkId(networkId = savedToken.networkId)
                if (blockchain == null) {
                    Timber.e("Token $savedToken has unknown networkId")
                    return@groupBy null
                }

                val accountNodeRecognizer = AccountNodeRecognizer(blockchain)
                val accountIndex = accountNodeRecognizer.recognize(derivationPathValue)
                if (accountIndex == null) {
                    Timber.e("Token $savedToken has unrecognized derivation path")
                    return@groupBy null
                }

                accountIndex
            }
    }

    private fun Map<Long?, List<UserTokensResponse.Token>>.mapKeysToAccountId(
        userWalletId: UserWalletId,
    ): Map<AccountId?, List<UserTokensResponse.Token>> {
        return mapKeys { (accountIndex, _) ->
            if (accountIndex == null) return@mapKeys null

            val derivationIndex = DerivationIndex.invoke(value = accountIndex.toInt()).getOrElse {
                Timber.e("Failed to parse derivation index from account index: $accountIndex")
                return@mapKeys null
            }

            AccountId.forCryptoPortfolio(userWalletId, derivationIndex)
        }
    }

    private fun Map<AccountId?, List<UserTokensResponse.Token>>.mapToEnrichedTokens(): List<UserTokensResponse.Token> {
        return flatMap { (accountId, tokens) ->
            if (accountId == null) return@flatMap emptyList()

            tokens.map { it.copy(accountId = accountId.value) }
        }
    }
}