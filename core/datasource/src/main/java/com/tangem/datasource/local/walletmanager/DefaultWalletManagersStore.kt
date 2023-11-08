package com.tangem.datasource.local.walletmanager

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.WalletManager
import com.tangem.datasource.local.datastore.core.StringKeyDataStore
import com.tangem.datasource.local.datastore.core.StringKeyDataStoreDecorator
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.extensions.addOrReplace
import com.tangem.utils.extensions.removeBy
import kotlinx.coroutines.flow.Flow

internal class DefaultWalletManagersStore(
    dataStore: StringKeyDataStore<List<WalletManager>>,
) : WalletManagersStore, StringKeyDataStoreDecorator<UserWalletId, List<WalletManager>>(dataStore) {

    override fun provideStringKey(key: UserWalletId): String {
        return key.stringValue
    }

    override fun getAll(userWalletId: UserWalletId): Flow<List<WalletManager>> {
        return get(key = userWalletId)
    }

    override suspend fun getSyncOrNull(
        userWalletId: UserWalletId,
        blockchain: Blockchain,
        derivationPath: String?,
    ): WalletManager? {
        val walletManagers = getSyncOrNull(userWalletId)

        return walletManagers?.singleOrNull {
            it.wallet.blockchain == blockchain &&
                it.wallet.publicKey.derivationPath?.rawPath == derivationPath
        }
    }

    override suspend fun getAllSync(userWalletId: UserWalletId): List<WalletManager> {
        return getSyncOrNull(userWalletId) ?: emptyList()
    }

    override suspend fun store(userWalletId: UserWalletId, walletManager: WalletManager) {
        val walletManagers = getSyncOrNull(userWalletId)

        val updatedWalletManagers = walletManagers
            ?.addOrReplace(walletManager) {
                it.wallet.blockchain == walletManager.wallet.blockchain &&
                    it.wallet.publicKey.derivationPath == walletManager.wallet.publicKey.derivationPath
            }
            ?: listOf(walletManager)

        store(userWalletId, updatedWalletManagers)
    }

    override suspend fun remove(userWalletId: UserWalletId, predicate: (WalletManager) -> Boolean) {
        val walletManagers = getSyncOrNull(userWalletId)?.toMutableList() ?: return

        walletManagers.removeBy(predicate)

        store(userWalletId, walletManagers)
    }
}