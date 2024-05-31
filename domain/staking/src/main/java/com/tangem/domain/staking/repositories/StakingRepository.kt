package com.tangem.domain.staking.repositories

import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingEntryInfo
import com.tangem.domain.staking.model.Yield

interface StakingRepository {

    fun getStakingAvailability(blockchainId: String): StakingAvailability

    suspend fun getEntryInfo(integrationId: String): StakingEntryInfo

    suspend fun fetchEnabledYields()

    suspend fun getEnabledYields() : List<Yield>?
}
