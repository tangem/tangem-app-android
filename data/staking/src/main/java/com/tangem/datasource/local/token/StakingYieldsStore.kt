package com.tangem.datasource.local.token

import com.tangem.grow.datasource.stakekit.models.response.model.YieldDTO
import kotlinx.coroutines.flow.Flow

interface StakingYieldsStore {

    fun get(): Flow<List<YieldDTO>>

    suspend fun getSync(): List<YieldDTO>

    suspend fun getSyncWithTimeout(): List<YieldDTO>?

    suspend fun store(items: List<YieldDTO>, force: Boolean = false)
}