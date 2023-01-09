package com.tangem.tap.domain.userWalletList.repository.implementation

import com.tangem.common.services.secure.SecureStorage
import com.tangem.domain.common.util.UserWalletId
import com.tangem.tap.domain.userWalletList.repository.SelectedUserWalletRepository

internal class DefaultSelectedUserWalletRepository(
    private val secureStorage: SecureStorage,
) : SelectedUserWalletRepository {
    override fun get(): UserWalletId? {
        return secureStorage.get(StorageKey.SelectedWalletId.name)
            ?.decodeToString(throwOnInvalidSequence = true)
            ?.let { UserWalletId(it) }
    }

    override fun set(walletId: UserWalletId?) {
        if (walletId == null) {
            secureStorage.delete(StorageKey.SelectedWalletId.name)
        } else {
            secureStorage.store(
                data = walletId.stringValue.encodeToByteArray(throwOnInvalidSequence = true),
                account = StorageKey.SelectedWalletId.name,
            )
        }
    }

    private enum class StorageKey {
        SelectedWalletId
    }
}
