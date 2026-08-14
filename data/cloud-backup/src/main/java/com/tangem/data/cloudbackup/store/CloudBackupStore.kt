package com.tangem.data.cloudbackup.store

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow

internal interface CloudBackupStore {

    fun getBackedUpWalletIds(): Flow<Set<String>>

    suspend fun setBackedUp(walletId: String, backedUp: Boolean)
}

internal class DefaultCloudBackupStore(
    private val dataStore: DataStore<Set<String>>,
) : CloudBackupStore {

    override fun getBackedUpWalletIds(): Flow<Set<String>> = dataStore.data

    override suspend fun setBackedUp(walletId: String, backedUp: Boolean) {
        dataStore.updateData { current ->
            if (backedUp) current + walletId else current - walletId
        }
    }
}