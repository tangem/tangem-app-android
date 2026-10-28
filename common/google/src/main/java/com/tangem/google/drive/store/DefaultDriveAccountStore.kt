package com.tangem.google.drive.store

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.first

internal class DefaultDriveAccountStore(
    private val dataStore: DataStore<Map<String, String>>,
) : DriveAccountStore {

    override suspend fun getAccountName(walletId: String): String? = dataStore.data.first()[walletId]

    override suspend fun setAccountName(walletId: String, accountName: String?) {
        dataStore.updateData { accounts ->
            if (accountName == null) accounts - walletId else accounts + (walletId to accountName)
        }
    }
}