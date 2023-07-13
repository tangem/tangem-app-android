package com.tangem.data.tokens.repository

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.data.tokens.mock.MockTokens
import com.tangem.domain.tokens.error.TokensError
import com.tangem.domain.tokens.model.Token
import com.tangem.domain.tokens.repository.TokensRepository
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal class MockTokensRepository : TokensRepository {

    override suspend fun sortTokens(
        userWalletId: UserWalletId,
        sortedTokensIds: Set<Token.ID>,
        isGrouped: Boolean,
        isSortedByBalance: Boolean,
    ): Either<TokensError, Unit> = Unit.right()

    override fun getTokens(userWalletId: UserWalletId, refresh: Boolean): Flow<Either<TokensError, Set<Token>>> {
        return flowOf(value = MockTokens.tokens[userWalletId]?.right() ?: TokensError.EmptyTokens.left())
    }

    override fun isTokensGrouped(userWalletId: UserWalletId): Flow<Either<TokensError, Boolean>> {
        return flowOf(MockTokens.isGrouped[userWalletId]?.right() ?: TokensError.EmptyTokens.left())
    }

    override fun isTokensSortedByBalance(userWalletId: UserWalletId): Flow<Either<TokensError, Boolean>> {
        return flowOf(MockTokens.isSortedByBalance[userWalletId]?.right() ?: TokensError.EmptyTokens.left())
    }
}
