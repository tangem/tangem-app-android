package com.tangem.data.tokens.repository

import arrow.core.Either
import com.tangem.domain.tokens.error.TokensError
import com.tangem.domain.tokens.model.Token
import com.tangem.domain.tokens.repository.TokensRepository
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

internal class DefaultTokensRepository : TokensRepository {

    override suspend fun sortTokens(
        userWalletId: UserWalletId,
        sortedTokensIds: Set<Token.ID>,
        isGrouped: Boolean,
        isSortedByBalance: Boolean,
    ): Either<TokensError, Unit> {
        TODO("Not yet implemented")
    }

    override fun getTokens(userWalletId: UserWalletId, refresh: Boolean): Flow<Either<TokensError, Set<Token>>> {
        TODO("Not yet implemented")
    }

    override fun isTokensGrouped(userWalletId: UserWalletId): Flow<Either<TokensError, Boolean>> {
        TODO("Not yet implemented")
    }

    override fun isTokensSortedByBalance(userWalletId: UserWalletId): Flow<Either<TokensError, Boolean>> {
        TODO("Not yet implemented")
    }
}