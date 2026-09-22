package com.tangem.data.sociallogin.store

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow

/**
 * Local record of which wallets have social recovery fully set up. Kept on device so backup checks
 * never wait on the cloud or the backend.
 */
internal interface SocialBackupStore {

    fun getCompletedWalletIds(): Flow<Set<String>>

    suspend fun setCompleted(walletId: String, isCompleted: Boolean)
}

internal class DefaultSocialBackupStore(
    private val dataStore: DataStore<Set<String>>,
) : SocialBackupStore {

    override fun getCompletedWalletIds(): Flow<Set<String>> = dataStore.data

    override suspend fun setCompleted(walletId: String, isCompleted: Boolean) {
        dataStore.updateData { current ->
            if (isCompleted) current + walletId else current - walletId
        }
    }
}