package com.tangem.data.addressbook.store

import com.tangem.domain.addressbook.model.AddressBookBlob
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

interface AddressBookBlobStore {

    fun getBlob(userWalletId: UserWalletId): Flow<AddressBookBlob?>

    fun getBlobs(userWalletIds: Set<UserWalletId>): Flow<List<AddressBookBlob>>

    suspend fun getBlobSync(userWalletId: UserWalletId): AddressBookBlob?

    /** Persists [blob] optimistically with `isBESynchronized = false`. Keyed by [AddressBookBlob.walletId]. */
    suspend fun storeBlob(blob: AddressBookBlob)

    /** Flips the BE-sync flag to `true` once the backend confirms the push. No-op if the blob is absent. */
    suspend fun markAsSynchronized(userWalletId: UserWalletId)

    /** Blobs still pending a backend push — the entry point for the future sync service. */
    suspend fun getUnsynchronizedBlobs(): List<AddressBookBlob>

    suspend fun deleteBlob(userWalletId: UserWalletId)
}