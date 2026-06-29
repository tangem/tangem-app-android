package com.tangem.data.addressbook.store

import com.tangem.domain.addressbook.model.AddressBookBlob
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

interface AddressBookBlobStore {

    fun getBlob(userWalletId: UserWalletId): Flow<AddressBookBlob?>

    fun getBlobs(userWalletIds: Set<UserWalletId>): Flow<List<AddressBookBlob>>

    suspend fun getBlobSync(userWalletId: UserWalletId): AddressBookBlob?

    suspend fun storeBlob(blob: AddressBookBlob)

    suspend fun deleteBlob(userWalletId: UserWalletId)
}