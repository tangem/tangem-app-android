package com.tangem.datasource.local.token

import androidx.datastore.core.DataStore
import com.tangem.datasource.api.stakekit.models.response.model.YieldDTO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

internal class DefaultStakingYieldsStore(
    private val dataStore: DataStore<List<YieldDTO>>,
) : StakingYieldsStore {

    override fun get(): Flow<List<YieldDTO>> {
        return dataStore.data
    }

    override suspend fun getSync(): List<YieldDTO> {
        return dataStore.data.firstOrNull().orEmpty()
    }

    override suspend fun store(items: List<YieldDTO>) {
        dataStore.updateData { _ ->
            items
        }
    }
}