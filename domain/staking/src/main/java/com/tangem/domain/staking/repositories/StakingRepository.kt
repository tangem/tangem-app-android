package com.tangem.domain.staking.repositories

import com.tangem.domain.staking.model.*
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.NetworkStatus
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
        networkStatus: NetworkStatus,
        integrationId: String,
        refresh: Boolean = false,
    )

    fun getSingleYieldBalanceFlow(
        userWalletId: UserWalletId,
        networkStatus: NetworkStatus,
        integrationId: String,
    ): Flow<List<YieldBalance>>

    suspend fun fetchMultiYieldBalance(
        userWalletId: UserWalletId,
        networks: Set<NetworkStatus>,
        integrationId: String,
        refresh: Boolean = false,
    )

    fun getMultiYieldBalanceFlow(
        userWalletId: UserWalletId,
        networks: Set<NetworkStatus>,
        integrationId: String,
    ): Flow<List<YieldBalanceList>>
}