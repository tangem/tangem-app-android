package com.tangem.domain.staking.repositories

import com.tangem.domain.staking.model.StakingEntryInfo

interface StakingRepository {

    suspend fun getEntryInfo(integrationId: String): StakingEntryInfo
}
