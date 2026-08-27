package com.tangem.datasource.local.walletmanager

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.WalletManager
import com.tangem.core.local.datastore.RuntimeSharedMapStore
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.extensions.addOrReplace
import com.tangem.utils.extensions.removeBy
import kotlinx.coroutines.flow.Flow

internal class DefaultWalletManagersStore(
    private val store: RuntimeSharedMapStore<UserWalletId, List<WalletManager>>,
) : WalletManagersStore {

    override fun getAll(userWalletId: UserWalletId): Flow<List<WalletManager>> {
        return store.get(key = userWalletId)
    }

    override suspend fun getSyncOrNull(
        userWalletId: UserWalletId,
        blockchain: Blockchain,
        derivationPath: String?,
    ): WalletManager? {
        val walletManagers = store.getSyncOrNull(userWalletId)

        return walletManagers?.singleOrNull { walletManager ->
            walletManager.wallet.blockchain == blockchain &&
                walletManager.wallet.publicKey.derivationPath?.rawPath == derivationPath
        }
    }

    override suspend fun getAllSync(userWalletId: UserWalletId): List<WalletManager> {
        return store.getSyncOrNull(userWalletId).orEmpty()
    }

    override suspend fun store(userWalletId: UserWalletId, walletManager: WalletManager) {
        store.update(key = userWalletId, default = emptyList()) { walletManagers ->
            walletManagers.addOrReplace(walletManager) {
                it.wallet.blockchain == walletManager.wallet.blockchain &&
                    it.wallet.publicKey.derivationPath == walletManager.wallet.publicKey.derivationPath
            }
        }
    }

    override suspend fun remove(userWalletId: UserWalletId, predicate: (WalletManager) -> Boolean) {
        store.updateIfPresent(key = userWalletId) { walletManagers ->
            walletManagers.toMutableList().apply { removeBy(predicate) }
        }
    }

    override suspend fun clear() = store.clear()
}