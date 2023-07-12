package com.tangem.domain.tokens.repository

import arrow.core.Either
import com.tangem.domain.tokens.error.TokensError
import com.tangem.domain.tokens.model.Token
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

// FIXME: Use Raise as context instead of Effect when context receivers become stable
// [REDACTED_JIRA]

/**
 * Repository for everything related to the tokens of user wallet
 * */
interface TokensRepository {

    suspend fun sortTokens(
        userWalletId: UserWalletId,
        sortedTokensIds: Set<Token.ID>,
        isGrouped: Boolean,
        isSortedByBalance: Boolean,
    ): Either<TokensError, Unit>

    fun getTokens(userWalletId: UserWalletId, refresh: Boolean): Flow<Either<TokensError, Set<Token>>>

    fun isTokensGrouped(userWalletId: UserWalletId): Flow<Either<TokensError, Boolean>>

    fun isTokensSortedByBalance(userWalletId: UserWalletId): Flow<Either<TokensError, Boolean>>
}