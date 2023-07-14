package com.tangem.domain.tokens.repository

import com.tangem.domain.tokens.model.Token
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

// FIXME: Use Raise as context instead of Effect when context receivers become stable
// https://tangem.atlassian.net/browse/AND-3947

/**
 * Repository for everything related to the tokens of user wallet
 * */
interface TokensRepository {

    suspend fun sortTokens(
        userWalletId: UserWalletId,
        sortedTokensIds: Set<Token.ID>,
        isGrouped: Boolean,
        isSortedByBalance: Boolean,
    )

    fun getTokens(userWalletId: UserWalletId, refresh: Boolean): Flow<Set<Token>>

    fun isTokensGrouped(userWalletId: UserWalletId): Flow<Boolean>

    fun isTokensSortedByBalance(userWalletId: UserWalletId): Flow<Boolean>
}
