package com.tangem.data.wallets

import com.tangem.datasource.local.userwallet.ShouldSaveUserWalletStore
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

internal class DefaultWalletsRepository(
    private val shouldSaveUserWalletStore: ShouldSaveUserWalletStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : WalletsRepository {

    override suspend fun initialize() {
        withContext(dispatchers.io) {
            shouldSaveUserWalletStore.getSyncOrNull() ?: shouldSaveUserWalletStore.store(item = false)
        }
    }

    override suspend fun shouldSaveUserWalletsSync(): Boolean {
        return withContext(dispatchers.io) { shouldSaveUserWalletStore.getSyncOrNull() ?: false }
    }

    override fun shouldSaveUserWallets(): Flow<Boolean> = shouldSaveUserWalletStore.get()

    override suspend fun saveShouldSaveUserWallets(item: Boolean) {
        withContext(dispatchers.io) { shouldSaveUserWalletStore.store(item = item) }
    }
}