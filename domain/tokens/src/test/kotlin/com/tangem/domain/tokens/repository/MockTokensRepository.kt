package com.tangem.domain.tokens.repository

import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.domain.core.error.DataError
import com.tangem.domain.tokens.model.Token
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class MockTokensRepository(
    private val tokens: Flow<Either<DataError, Set<Token>>>,
    private val isGrouped: Flow<Either<DataError, Boolean>>,
    private val isSortedByBalance: Flow<Either<DataError, Boolean>>,
) : TokensRepository {

    override suspend fun sortTokens(
        userWalletId: UserWalletId,
        sortedTokens: Set<Token>,
        isGrouped: Boolean,
        isSortedByBalance: Boolean,
    ) = Unit

    override fun getTokens(userWalletId: UserWalletId, refresh: Boolean): Flow<Set<Token>> {
        return tokens.map { it.getOrElse { e -> throw e } }
    }

    override fun isTokensGrouped(userWalletId: UserWalletId): Flow<Boolean> {
        return isGrouped.map { it.getOrElse { e -> throw e } }
    }

    override fun isTokensSortedByBalance(userWalletId: UserWalletId): Flow<Boolean> {
        return isSortedByBalance.map { it.getOrElse { e -> throw e } }
    }
}