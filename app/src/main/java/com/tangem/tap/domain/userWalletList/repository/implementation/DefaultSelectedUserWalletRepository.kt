package com.tangem.tap.domain.userWalletList.repository.implementation

import com.tangem.common.services.secure.SecureStorage
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.tap.domain.userWalletList.repository.SelectedUserWalletRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

internal class DefaultSelectedUserWalletRepository(
    private val secureStorage: SecureStorage,
    private val dispatchers: CoroutineDispatcherProvider,
) : SelectedUserWalletRepository {
    override suspend fun get(): UserWalletId? = withContext(dispatchers.io) {
        secureStorage.get(StorageKey.SelectedWalletId.name)
            ?.decodeToString(throwOnInvalidSequence = true)
            ?.let { UserWalletId(it) }
    }

    override suspend fun set(walletId: UserWalletId?) = withContext(dispatchers.io) {
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
        SelectedWalletId,
    }
}