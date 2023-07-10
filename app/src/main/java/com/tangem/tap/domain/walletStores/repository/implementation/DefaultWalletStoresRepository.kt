package com.tangem.tap.domain.walletStores.repository.implementation

import com.tangem.common.CompletionResult
import com.tangem.common.catching
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.tap.domain.model.WalletStoreModel
import com.tangem.tap.domain.walletStores.repository.WalletStoresRepository
import com.tangem.tap.domain.walletStores.repository.implementation.utils.isSameWalletStore
import com.tangem.tap.domain.walletStores.repository.implementation.utils.replaceWalletStore
import com.tangem.tap.domain.walletStores.repository.implementation.utils.updateWithSelf
import com.tangem.tap.domain.walletStores.storage.WalletStoresStorage
import com.tangem.tap.features.wallet.models.Currency
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
        currentBlockchains: List<Currency.Blockchain>,
    ): CompletionResult<Unit> = catching {
        if (currentBlockchains != getSync(userWalletId)) {
            walletStoresStorage.update { prevStores ->
                prevStores.apply {
                    this[userWalletId] = this[userWalletId]
                        ?.filter { it.blockchainWalletData.currency in currentBlockchains }
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

    override suspend fun update(
        userWalletId: UserWalletId,
        operation: (List<WalletStoreModel>) -> WalletStoreModel?,
    ): CompletionResult<Unit> = catching {
        val walletStores = getSync(userWalletId).toMutableList()
        val updatedWalletStore = operation(walletStores) ?: return CompletionResult.Success(Unit)

        val index = walletStores.indexOfFirst { it.isSameWalletStore(updatedWalletStore) }
        if (index == -1 || updatedWalletStore == walletStores[index]) return CompletionResult.Success(Unit)

        walletStores[index] = updatedWalletStore

        walletStoresStorage.update { prevStores ->
            prevStores.apply {
                this[userWalletId] = walletStores
            }
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
