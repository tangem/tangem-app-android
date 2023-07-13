package com.tangem.domain.tokens.repository

import arrow.core.Either
import arrow.core.right
import com.tangem.domain.tokens.error.TokensError
import com.tangem.domain.tokens.model.Token
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

internal class MockTokensRepository(
    private val tokens: Flow<Either<TokensError, Set<Token>>>,
    private val isGrouped: Flow<Either<TokensError, Boolean>>,
    private val isSortedByBalance: Flow<Either<TokensError, Boolean>>,
) : TokensRepository {

    override suspend fun sortTokens(
        userWalletId: UserWalletId,
        sortedTokensIds: Set<Token.ID>,
        isGrouped: Boolean,
        isSortedByBalance: Boolean,
    ): Either<TokensError, Unit> = Unit.right()

    override fun getTokens(userWalletId: UserWalletId, refresh: Boolean): Flow<Either<TokensError, Set<Token>>> {
        return tokens
    }

    override fun isTokensGrouped(userWalletId: UserWalletId): Flow<Either<TokensError, Boolean>> {
        return isGrouped
    }

    override fun isTokensSortedByBalance(userWalletId: UserWalletId): Flow<Either<TokensError, Boolean>> {
        return isSortedByBalance
    }
}
