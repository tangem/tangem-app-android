package com.tangem.datasource.local.walletmanager

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.WalletManager
import com.tangem.datasource.local.datastore.RuntimeDataStore
import com.tangem.datasource.local.walletmanager.model.StoredWalletManagers
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.extensions.plusOrReplace

internal class RuntimeWalletManagersStore : WalletManagersStore {

    private val store = RuntimeDataStore(
        keyProvider = StoredWalletManagers::userWalletId,
    )

    override suspend fun getOrNull(
        userWalletId: UserWalletId,
        blockchain: Blockchain,
        derivationPath: String?,
    ): WalletManager? {
        val walletManagers = store.getSync { it.userWalletId == userWalletId }
            .singleOrNull()
            ?.walletManagers

        return walletManagers?.singleOrNull {
            it.wallet.blockchain == blockchain &&
                it.wallet.publicKey.derivationPath?.rawPath == derivationPath
        }
    }

    override suspend fun store(userWalletId: UserWalletId, walletManager: WalletManager) {
        val walletManagers = store.getSync { it.userWalletId == userWalletId }
            .singleOrNull()
            ?.walletManagers

        val updatedWalletManagers = walletManagers
            ?.plusOrReplace(walletManager) {
                it.wallet.blockchain == walletManager.wallet.blockchain &&
                    it.wallet.publicKey == walletManager.wallet.publicKey
            }
            ?: listOf(walletManager)

        store.addOrReplace(item = StoredWalletManagers(userWalletId, updatedWalletManagers))
    }

    override suspend fun clear() {
        store.clear()
    }
}
