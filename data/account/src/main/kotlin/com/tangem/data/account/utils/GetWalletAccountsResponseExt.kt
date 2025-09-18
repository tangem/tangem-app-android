package com.tangem.data.account.utils

import com.tangem.data.common.currency.UserTokensResponseAccountIdEnricher
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.domain.models.wallet.UserWalletId

/** Flattens the tokens from all wallet accounts into a single list */
internal fun GetWalletAccountsResponse.flattenTokens(): List<UserTokensResponse.Token> {
    return accounts.flatMap { it.tokens.orEmpty() }
}

/** Converts the [GetWalletAccountsResponse] into a [UserTokensResponse] */
internal fun GetWalletAccountsResponse.toUserTokensResponse(): UserTokensResponse {
    return UserTokensResponse(
        group = wallet.group,
        sort = wallet.sort,
        tokens = flattenTokens(),
    )
}

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
    val enrichedTokens = UserTokensResponseAccountIdEnricher(userWalletId, tokens)
        .groupBy { it.accountId }

    return map { accountDTO ->
        accountDTO.copy(
            tokens = enrichedTokens[accountDTO.id].orEmpty(),
        )
    }
}