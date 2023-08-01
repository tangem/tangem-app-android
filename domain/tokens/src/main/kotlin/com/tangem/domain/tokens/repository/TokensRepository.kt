package com.tangem.domain.tokens.repository

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

/**
 * Repository for everything related to the tokens of user wallet
 * */
interface TokensRepository {

    suspend fun saveTokens(
        userWalletId: UserWalletId,
        currencies: Set<CryptoCurrency>,
        isGroupedByNetwork: Boolean,
        isSortedByBalance: Boolean,
    )

    suspend fun getPrimaryCurrency(userWalletId: UserWalletId): CryptoCurrency

    fun getMultiCurrencyWalletCurrencies(userWalletId: UserWalletId, refresh: Boolean): Flow<Set<CryptoCurrency>>

    fun isTokensGrouped(userWalletId: UserWalletId): Flow<Boolean>

    fun isTokensSortedByBalance(userWalletId: UserWalletId): Flow<Boolean>
}