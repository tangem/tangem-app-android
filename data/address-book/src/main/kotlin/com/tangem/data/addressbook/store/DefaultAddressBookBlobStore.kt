package com.tangem.data.addressbook.store

import androidx.datastore.core.DataStore
import com.tangem.domain.addressbook.model.AddressBookBlob
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

internal typealias AddressBookBlobs = Map<String, StoredAddressBookBlob>

internal class DefaultAddressBookBlobStore(
    private val dataStore: DataStore<AddressBookBlobs>,
) : AddressBookBlobStore {

    override fun getBlob(userWalletId: UserWalletId): Flow<AddressBookBlob?> {
        return dataStore.data
            .map { it[userWalletId.stringValue]?.blob }
            .distinctUntilChanged()
    }

    override fun getBlobs(userWalletIds: Set<UserWalletId>): Flow<List<AddressBookBlob>> {
        val ids = userWalletIds.mapTo(mutableSetOf()) { it.stringValue }
        return dataStore.data
            .map { stored -> stored.filterKeys { it in ids }.values.map { it.blob } }
            .distinctUntilChanged()
    }

    override suspend fun getBlobSync(userWalletId: UserWalletId): AddressBookBlob? {
        return getStoredBlobs()[userWalletId.stringValue]?.blob
    }

    override suspend fun storeBlob(blob: AddressBookBlob) {
        dataStore.updateData { stored ->
            stored + (blob.walletId to StoredAddressBookBlob(blob = blob, isBESynchronized = false))
        }
    }

    override suspend fun markAsSynchronized(userWalletId: UserWalletId) {
        dataStore.updateData { stored ->
            val current = stored[userWalletId.stringValue] ?: return@updateData stored
            stored + (userWalletId.stringValue to current.copy(isBESynchronized = true))
        }
    }

    override suspend fun getUnsynchronizedBlobs(): List<AddressBookBlob> {
        return getStoredBlobs().values
            .filterNot { it.isBESynchronized }
            .map { it.blob }
    }

    override suspend fun deleteBlob(userWalletId: UserWalletId) {
        dataStore.updateData { stored -> stored - userWalletId.stringValue }
    }

    private suspend fun getStoredBlobs(): AddressBookBlobs = dataStore.data.first()
}