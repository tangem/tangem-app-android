package com.tangem.datasource.local.token

import androidx.datastore.core.DataStore
import com.tangem.domain.staking.model.ethpool.P2PEthPoolVault
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

internal class DefaultP2PEthPoolVaultsStore(
    private val dataStore: DataStore<List<P2PEthPoolVault>>,
) : P2PEthPoolVaultsStore {

    override fun get(): Flow<List<P2PEthPoolVault>> {
        return dataStore.data
    }

    override suspend fun getSync(): List<P2PEthPoolVault> {
        return dataStore.data.firstOrNull().orEmpty()
    }

    override suspend fun store(vaults: List<P2PEthPoolVault>) {
        dataStore.updateData { vaults }
    }
}