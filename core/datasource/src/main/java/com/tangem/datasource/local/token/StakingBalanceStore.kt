package com.tangem.datasource.local.token

import com.tangem.datasource.api.stakekit.models.response.model.BalanceDTO
import kotlinx.coroutines.flow.Flow

interface StakingBalanceStore {

    fun get(): Flow<List<BalanceDTO>>

    suspend fun getSyncOrNull(): List<BalanceDTO>?

    suspend fun store(items: List<BalanceDTO>)
}