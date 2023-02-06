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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class DefaultWalletStoresRepository : WalletStoresRepository {
    private val walletStoresStorage = WalletStoresStorage

    override fun getAll(): Flow<Map<UserWalletId, List<WalletStoreModel>>> {
        return walletStoresStorage.getAll()
    }

    override fun get(userWalletId: UserWalletId): Flow<List<WalletStoreModel>> {
        return getAll().map { it[userWalletId].orEmpty() }
    }

    override suspend fun getSync(userWalletId: UserWalletId): List<WalletStoreModel> {
        return get(userWalletId).firstOrNull() ?: emptyList()
    }

    override suspend fun contains(userWalletId: UserWalletId): Boolean {
        return getSync(userWalletId).isNotEmpty()
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
        if (currentBlockchains != getSync(userWalletId).map { it.blockchain }) {
            walletStoresStorage.update { prevStores ->
                prevStores.apply {
                    this[userWalletId] = this[userWalletId]
                        ?.filter { it.blockchain in currentBlockchains }
                        .orEmpty()
                }
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
        val currentWalletStores = this@addOrUpdate
        val userWalletStores = currentWalletStores[userWalletId]

        if (userWalletStores.isNullOrEmpty()) {
            currentWalletStores.apply {
                set(userWalletId, listOf(walletStore))
            }
        } else {
            val currentWalletStore = userWalletStores.find(walletStore::isSameWalletStore)
            if (currentWalletStore == null) {
                currentWalletStores.apply {
                    set(userWalletId, userWalletStores + walletStore)
                }
            } else {
                currentWalletStores.replaceWalletStore(
                    walletStoreToUpdate = currentWalletStore,
                    update = { it.updateWithSelf(walletStore) },
                )
            }
        }
    }
}
