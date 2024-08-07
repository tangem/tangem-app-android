package com.tangem.datasource.local.token

import com.tangem.datasource.api.stakekit.models.response.model.BalanceDTO
import com.tangem.datasource.api.stakekit.models.response.model.YieldBalanceWrapperDTO
import kotlinx.coroutines.flow.Flow

interface StakingBalanceStore {

    fun get(): Flow<List<YieldBalanceWrapperDTO>>

    suspend fun getSyncOrNull(): List<YieldBalanceWrapperDTO>?

    suspend fun store(items: List<YieldBalanceWrapperDTO>)

    fun get(integrationId: String): Flow<List<BalanceDTO>>

    suspend fun getSyncOrNull(integrationId: String): List<BalanceDTO>?

    suspend fun store(integrationId: String, item: YieldBalanceWrapperDTO)
}