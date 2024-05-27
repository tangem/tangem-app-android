package com.tangem.domain.staking.repositories

import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingEntryInfo

interface StakingRepository {

    // TODO staking move to other repo?
    fun getStakingAvailability(blockchainId: String) : StakingAvailability

    suspend fun getEntryInfo(integrationId: String): StakingEntryInfo
}
