package com.tangem.data.account.utils

import com.tangem.data.common.currency.UserTokensResponseAccountIdEnricher
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Assigns tokens from a [UserTokensResponse] to the wallet accounts in the [GetWalletAccountsResponse]
 *
 * @param userWalletId the ID of the user wallet
 *
 * @return a new [GetWalletAccountsResponse]` with tokens assigned to the wallet accounts
 */
internal fun GetWalletAccountsResponse.assignTokens(userWalletId: UserWalletId): GetWalletAccountsResponse {
    return copy(
        accounts = accounts.assignTokens(userWalletId = userWalletId, tokens = unassignedTokens),
        unassignedTokens = emptyList(),
    )
}

/**
 * Assigns tokens from a [UserTokensResponse] to a list of wallet accounts
 *
 * @param userWalletId the ID of the user wallet
 * @param tokens       tokens to be assigned
 *
 * @return a new list of [WalletAccountDTO] with tokens assigned to each account
 */
internal fun List<WalletAccountDTO>.assignTokens(
    userWalletId: UserWalletId,
    tokens: List<UserTokensResponse.Token>,
): List<WalletAccountDTO> {
    val enrichedTokensByAccountId = UserTokensResponseAccountIdEnricher(userWalletId, tokens)
        .groupBy { it.accountId }

    return map { accountDTO ->
        val accountTokens = enrichedTokensByAccountId[accountDTO.id].orEmpty()
        val isMainAccount = accountDTO.derivationIndex == 0

        val tokens = if (isMainAccount) {
            val existingAccountIds = map(WalletAccountDTO::id).toSet()
            val unexistingAccountIds = enrichedTokensByAccountId.keys - existingAccountIds

            val customTokens = unexistingAccountIds.flatMap {
                enrichedTokensByAccountId[it].orEmpty().map { token ->
                    // Tokens from unexisting accounts should be copied to the main account
                    token.copy(accountId = accountDTO.id)
                }
            }

            accountTokens + customTokens
        } else {
            accountTokens
        }

        accountDTO.copy(tokens = accountDTO.tokens.orEmpty() + tokens)
    }
}

internal fun WalletAccountDTO.assignTokens(
    userWalletId: UserWalletId,
    tokens: List<UserTokensResponse.Token>,
): WalletAccountDTO {
    val enrichedTokens = UserTokensResponseAccountIdEnricher(userWalletId, tokens)
        .filter { it.accountId == this.id }

    return copy(tokens = this.tokens.orEmpty() + enrichedTokens)
}