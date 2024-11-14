package com.tangem.datasource.local.token

import com.tangem.datasource.api.stakekit.models.response.model.YieldDTO
import com.tangem.datasource.local.datastore.core.StringKeyDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class DefaultStakingYieldsStore(
    private val dataStore: StringKeyDataStore<List<YieldDTO>>,
) : StakingYieldsStore {

    private val mutex = Mutex()

    override fun get(): Flow<List<YieldDTO>> {
        return dataStore.get(KEY)
    }

    override suspend fun getSync(): List<YieldDTO> {
        return dataStore.getSyncOrNull(KEY) ?: emptyList()
    }

    override suspend fun store(items: List<YieldDTO>) {
        mutex.withLock {
            dataStore.store(KEY, items)
        }
    }

    companion object {
        private const val KEY = "DefaultStakingYieldsStore"
    }
}