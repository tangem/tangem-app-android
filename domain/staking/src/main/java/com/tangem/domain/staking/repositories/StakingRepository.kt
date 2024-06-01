package com.tangem.domain.staking.repositories

import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingEntryInfo

interface StakingRepository {

    suspend fun getStakingAvailabilityForActions(currencyId: String, symbol: String): StakingAvailability

    suspend fun getEntryInfo(integrationId: String): StakingEntryInfo

    suspend fun fetchEnabledYields()

    fun isStakingSupported(currencyId: String): Boolean
}
