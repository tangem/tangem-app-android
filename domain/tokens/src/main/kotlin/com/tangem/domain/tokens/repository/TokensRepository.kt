package com.tangem.domain.tokens.repository

import com.tangem.domain.tokens.model.Token
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

/**
 * Repository for everything related to the tokens of user wallet
 * */
interface TokensRepository {

    suspend fun saveTokens(
        userWalletId: UserWalletId,
        tokens: Set<Token>,
        isGroupedByNetwork: Boolean,
        isSortedByBalance: Boolean,
    )

    suspend fun getSingleCurrencyWalletToken(userWalletId: UserWalletId): Token

    fun getMultiCurrencyWalletTokens(userWalletId: UserWalletId, refresh: Boolean): Flow<Set<Token>>

    fun isTokensGrouped(userWalletId: UserWalletId): Flow<Boolean>

    fun isTokensSortedByBalance(userWalletId: UserWalletId): Flow<Boolean>
}