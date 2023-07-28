package com.tangem.datasource.local.walletmanager

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.WalletManager
import com.tangem.datasource.local.datastore.core.StringKeyDataStore
import com.tangem.datasource.local.datastore.core.StringKeyDataStoreDecorator
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.extensions.plusOrReplace

internal class DefaultWalletManagersStore(
    dataStore: StringKeyDataStore<List<WalletManager>>,
) : WalletManagersStore, StringKeyDataStoreDecorator<UserWalletId, List<WalletManager>>(dataStore) {

    override fun provideStringKey(key: UserWalletId): String {
        return key.stringValue
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

    override suspend fun store(userWalletId: UserWalletId, walletManager: WalletManager) {
        val walletManagers = getSyncOrNull(userWalletId)

        val updatedWalletManagers = walletManagers
            ?.plusOrReplace(walletManager) {
                it.wallet.blockchain == walletManager.wallet.blockchain &&
                    it.wallet.publicKey == walletManager.wallet.publicKey
            }
            ?: listOf(walletManager)

        store(userWalletId, updatedWalletManagers)
    }
}