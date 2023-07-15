package com.tangem.data.tokens.repository

import com.tangem.data.tokens.mock.MockTokens
import com.tangem.domain.tokens.model.Token
import com.tangem.domain.tokens.repository.TokensRepository
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal class MockTokensRepository : TokensRepository {

    override suspend fun sortTokens(
        userWalletId: UserWalletId,
        sortedTokens: Set<Token>,
        isGrouped: Boolean,
        isSortedByBalance: Boolean,
    ) = Unit

    override fun getTokens(userWalletId: UserWalletId, refresh: Boolean): Flow<Set<Token>> {
        return flowOf(value = MockTokens.tokens[userWalletId]!!)
    }

    override fun isTokensGrouped(userWalletId: UserWalletId): Flow<Boolean> {
        return flowOf(value = MockTokens.isGrouped[userWalletId]!!)
    }

    override fun isTokensSortedByBalance(userWalletId: UserWalletId): Flow<Boolean> {
        return flowOf(value = MockTokens.isSortedByBalance[userWalletId]!!)
    }
}