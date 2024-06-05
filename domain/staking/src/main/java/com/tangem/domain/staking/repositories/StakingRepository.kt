package com.tangem.domain.staking.repositories

import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingEntryInfo
import com.tangem.domain.tokens.model.CryptoCurrency

interface StakingRepository {

    suspend fun getStakingAvailabilityForActions(
        cryptoCurrencyId: CryptoCurrency.ID,
        symbol: String,
    ): StakingAvailability

    suspend fun getEntryInfo(integrationId: String): StakingEntryInfo

    suspend fun fetchEnabledYields()

    fun isStakingSupported(currencyId: String): Boolean
}