package com.tangem.tap.domain.walletStores.repository.implementation

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.CompletionResult
import com.tangem.common.catching
import com.tangem.domain.common.util.UserWalletId
import com.tangem.tap.domain.model.WalletStoreModel
import com.tangem.tap.domain.walletStores.repository.WalletStoresRepository
import com.tangem.tap.domain.walletStores.repository.implementation.utils.isSameWalletStore
import com.tangem.tap.domain.walletStores.repository.implementation.utils.replaceWalletStore
import com.tangem.tap.domain.walletStores.repository.implementation.utils.updateWithSelf
import com.tangem.tap.domain.walletStores.storage.WalletStoresStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

internal class DefaultWalletStoresRepository : WalletStoresRepository {
    private val walletStoresStorage = WalletStoresStorage

    override fun getAll(): Flow<Map<UserWalletId, List<WalletStoreModel>>> {
        return walletStoresStorage.getAll()
    }

    override fun get(userWalletId: UserWalletId): Flow<List<WalletStoreModel>> {
        return walletStoresStorage.get(userWalletId)
    }

    override suspend fun contains(userWalletId: UserWalletId): Boolean {
        return walletStoresStorage.getSync(userWalletId).isNotEmpty()
    }

    override suspend fun delete(userWalletsIds: List<UserWalletId>): CompletionResult<Unit> = catching {
        walletStoresStorage.update { prevStores ->
            prevStores.filterKeys { it !in userWalletsIds } as HashMap<UserWalletId, List<WalletStoreModel>>
        }
    }

    override suspend fun deleteDifference(
        userWalletId: UserWalletId,
        currentBlockchains: List<Blockchain>,
    ): CompletionResult<Unit> = catching {
        walletStoresStorage.update { prevStores ->
            prevStores.apply {
                this[userWalletId] = this[userWalletId]
                    ?.filter { it.blockchainNetwork.blockchain in currentBlockchains }
                    .orEmpty()
            }
        }
    }

    override suspend fun clear(): CompletionResult<Unit> = catching {
        walletStoresStorage.update { hashMapOf() }
    }

    override suspend fun storeOrUpdate(
        userWalletId: UserWalletId,
        walletStore: WalletStoreModel,
    ): CompletionResult<Unit> = catching {
        walletStoresStorage.update { prevStores ->
            prevStores.addOrUpdate(userWalletId, walletStore)
        }
    }

    private suspend fun HashMap<UserWalletId, List<WalletStoreModel>>.addOrUpdate(
        userWalletId: UserWalletId,
        walletStore: WalletStoreModel,
    ): HashMap<UserWalletId, List<WalletStoreModel>> = withContext(Dispatchers.Default) {
        val prevStores = this@addOrUpdate
        val walletStores = prevStores[userWalletId]

        if (walletStores.isNullOrEmpty()) {
            prevStores.apply {
                set(userWalletId, listOf(walletStore))
            }
        } else {
            val oldWalletStore = walletStores.find(walletStore::isSameWalletStore)

            if (oldWalletStore == null) {
                prevStores.apply {
                    set(userWalletId, walletStores + walletStore)
                }
            } else {
                prevStores.replaceWalletStore(
                    walletId = userWalletId,
                    walletStore = oldWalletStore,
                    update = { it.updateWithSelf(walletStore) },
                )
            }
        }
    }
}
