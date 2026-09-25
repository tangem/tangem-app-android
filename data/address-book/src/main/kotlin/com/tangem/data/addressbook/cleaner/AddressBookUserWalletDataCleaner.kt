package com.tangem.data.addressbook.cleaner

import com.tangem.data.addressbook.store.AddressBookBlobStore
import com.tangem.domain.common.wallets.UserWalletDataCleaner
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject

/**
 * Removes the locally cached (encrypted) address book of a deleted wallet. The ETag is already cleared by the accounts
 * cleaner; without this the blob itself stayed in DataStore for good.
 */
internal class AddressBookUserWalletDataCleaner @Inject constructor(
    private val blobStore: AddressBookBlobStore,
) : UserWalletDataCleaner {

    override suspend fun clear(userWalletIds: List<UserWalletId>) {
        userWalletIds.forEach { userWalletId ->
            runSuspendCatching { blobStore.deleteBlob(userWalletId) }
                .onFailure { TangemLogger.e("Failed to clear the address book of $userWalletId", it) }
        }
    }
}
