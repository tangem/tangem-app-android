package com.tangem.domain.staking.repositories

import com.tangem.domain.staking.model.*
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyAddress
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

interface StakingRepository {

    fun isStakingSupported(currencyId: String): Boolean

    suspend fun fetchEnabledYields()

    suspend fun getEntryInfo(integrationId: String): StakingEntryInfo

    suspend fun getYield(cryptoCurrencyId: CryptoCurrency.ID, symbol: String): Yield

    suspend fun getStakingAvailabilityForActions(
        cryptoCurrencyId: CryptoCurrency.ID,
        symbol: String,
    ): StakingAvailability

    suspend fun fetchSingleYieldBalance(
        userWalletId: UserWalletId,
        address: String,
        integrationId: String,
        refresh: Boolean = false,
    )

    fun getSingleYieldBalanceFlow(
        userWalletId: UserWalletId,
        address: String,
        integrationId: String,
    ): Flow<YieldBalance>

    suspend fun fetchMultiYieldBalance(
        userWalletId: UserWalletId,
        addresses: List<CryptoCurrencyAddress>,
        integrationId: String,
        refresh: Boolean = false,
    )

    fun getMultiYieldBalanceFlow(
        userWalletId: UserWalletId,
        addresses: List<CryptoCurrencyAddress>,
        integrationId: String,
    ): Flow<YieldBalanceList>
}