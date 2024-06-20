package com.tangem.domain.tokens.repository

import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.domain.core.error.DataError
import com.tangem.domain.core.lce.LceFlow
import com.tangem.domain.core.utils.toLce
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.FeePaidCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

internal class MockCurrenciesRepository(
    private val sortTokensResult: Either<DataError, Unit>,
    private val removeCurrencyResult: Either<DataError, Unit>,
    private val token: Either<DataError, CryptoCurrency>,
    private val tokens: Flow<Either<DataError, List<CryptoCurrency>>>,
    private val isGrouped: Flow<Either<DataError, Boolean>>,
    private val isSortedByBalance: Flow<Either<DataError, Boolean>>,
) : CurrenciesRepository {

    var tokensIdsAfterSortingApply: List<CryptoCurrency>? = null
        private set

    var isTokensGroupedAfterSortingApply: Boolean? = null
        private set

    var isTokensSortedByBalanceAfterSortingApply: Boolean? = null
        private set

    override suspend fun saveTokens(
        userWalletId: UserWalletId,
        currencies: List<CryptoCurrency>,
        isGroupedByNetwork: Boolean,
        isSortedByBalance: Boolean,
    ) {
        sortTokensResult.onLeft { throw it }

        tokensIdsAfterSortingApply = currencies
        isTokensGroupedAfterSortingApply = isGroupedByNetwork
        isTokensSortedByBalanceAfterSortingApply = isSortedByBalance
    }

    override suspend fun addCurrencies(userWalletId: UserWalletId, currencies: List<CryptoCurrency>) = Unit

    override suspend fun removeCurrency(userWalletId: UserWalletId, currency: CryptoCurrency) {
        removeCurrencyResult.onLeft { throw it }
    }

    override suspend fun removeCurrencies(userWalletId: UserWalletId, currencies: List<CryptoCurrency>) = Unit

    override fun getWalletCurrenciesUpdates(userWalletId: UserWalletId): LceFlow<Throwable, List<CryptoCurrency>> {
        return emptyFlow()
    }

    override suspend fun getMultiCurrencyWalletCurrenciesSync(
        userWalletId: UserWalletId,
        refresh: Boolean,
    ): List<CryptoCurrency> {
        return tokens.first().getOrElse { e -> throw e }
    }

    override suspend fun getSingleCurrencyWalletPrimaryCurrency(userWalletId: UserWalletId): CryptoCurrency {
        return token.getOrElse { e -> throw e }
    }

    override suspend fun getSingleCurrencyWalletWithCardCurrencies(userWalletId: UserWalletId): List<CryptoCurrency> {
        return tokens.first().getOrElse { e -> throw e }
    }

    override suspend fun getSingleCurrencyWalletWithCardCurrency(
        userWalletId: UserWalletId,
        id: CryptoCurrency.ID,
    ): CryptoCurrency {
        return token.getOrElse { e -> throw e }
    }

    override fun getMultiCurrencyWalletCurrenciesUpdates(userWalletId: UserWalletId): Flow<List<CryptoCurrency>> {
        return tokens.map { it.getOrElse { e -> throw e } }
    }

    override fun getMultiCurrencyWalletCurrenciesUpdatesLce(
        userWalletId: UserWalletId,
    ): LceFlow<Throwable, List<CryptoCurrency>> {
        return tokens.map { it.toLce() }
    }

    override suspend fun getMultiCurrencyWalletCurrency(
        userWalletId: UserWalletId,
        id: CryptoCurrency.ID,
    ): CryptoCurrency {
        val token = token.getOrElse { e -> throw e }

        require(token.id == id)

        return token
    }

    override fun getMissedAddressesCryptoCurrencies(userWalletId: UserWalletId): Flow<List<CryptoCurrency>> {
        /* no-op */
        return emptyFlow()
    }

    override suspend fun getNetworkCoin(
        userWalletId: UserWalletId,
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
    ): CryptoCurrency.Coin {
        TODO("Not yet implemented")
    }

    override fun isTokensGrouped(userWalletId: UserWalletId): Flow<Boolean> {
        return isGrouped.map { it.getOrElse { e -> throw e } }
    }

    override fun isTokensSortedByBalance(userWalletId: UserWalletId): Flow<Boolean> {
        return isSortedByBalance.map { it.getOrElse { e -> throw e } }
    }

    override fun isSendBlockedByPendingTransactions(
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        coinStatus: CryptoCurrencyStatus?,
    ): Boolean {
        return false
    }

    override suspend fun getFeePaidCurrency(userWalletId: UserWalletId, currency: CryptoCurrency): FeePaidCurrency {
        return FeePaidCurrency.Coin
    }

    override fun createTokenCurrency(cryptoCurrency: CryptoCurrency.Token, network: Network): CryptoCurrency.Token {
        return cryptoCurrency
    }
}