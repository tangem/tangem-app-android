package com.tangem.domain.tokens.repository

import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.domain.core.error.DataError
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class MockCurrenciesRepository(
    private val sortTokensResult: Either<DataError, Unit>,
    private val token: Either<DataError, CryptoCurrency>,
    private val tokens: Flow<Either<DataError, Set<CryptoCurrency>>>,
    private val isGrouped: Flow<Either<DataError, Boolean>>,
    private val isSortedByBalance: Flow<Either<DataError, Boolean>>,
) : CurrenciesRepository {

    var tokensIdsAfterSortingApply: Set<CryptoCurrency>? = null
        private set

    var isTokensGroupedAfterSortingApply: Boolean? = null
        private set

    var isTokensSortedByBalanceAfterSortingApply: Boolean? = null
        private set

    override suspend fun saveTokens(
        userWalletId: UserWalletId,
        currencies: Set<CryptoCurrency>,
        isGroupedByNetwork: Boolean,
        isSortedByBalance: Boolean,
    ) {
        sortTokensResult.onLeft { throw it }

        tokensIdsAfterSortingApply = currencies
        isTokensGroupedAfterSortingApply = isGroupedByNetwork
        isTokensSortedByBalanceAfterSortingApply = isSortedByBalance
    }

    override suspend fun getSingleCurrencyWalletPrimaryCurrency(userWalletId: UserWalletId): CryptoCurrency {
        return token.getOrElse { e -> throw e }
    }

    override fun getMultiCurrencyWalletCurrencies(
        userWalletId: UserWalletId,
        refresh: Boolean,
    ): Flow<Set<CryptoCurrency>> {
        return tokens.map { it.getOrElse { e -> throw e } }
    }

    override suspend fun getMultiCurrencyWalletCurrency(
        userWalletId: UserWalletId,
        id: CryptoCurrency.ID,
    ): CryptoCurrency {
        val token = token.getOrElse { e -> throw e }

        require(token.id == id)

        return token
    }

    override fun isTokensGrouped(userWalletId: UserWalletId): Flow<Boolean> {
        return isGrouped.map { it.getOrElse { e -> throw e } }
    }

    override fun isTokensSortedByBalance(userWalletId: UserWalletId): Flow<Boolean> {
        return isSortedByBalance.map { it.getOrElse { e -> throw e } }
    }
}
