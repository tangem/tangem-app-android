package com.tangem.tap.domain.walletStores.storage

import com.tangem.domain.common.util.UserWalletId
import com.tangem.tap.domain.model.WalletStoreModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal object WalletStoresStorage {
    private val stores =
        MutableSharedFlow<HashMap<UserWalletId, List<WalletStoreModel>>>(replay = 1)

    init {
        stores.tryEmit(hashMapOf())
    }

    fun getAll(): Flow<Map<UserWalletId, List<WalletStoreModel>>> {
        return stores
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun get(userWalletId: UserWalletId): Flow<List<WalletStoreModel>> {
        return stores
            .mapLatest { stores ->
                stores[userWalletId].orEmpty()
            }
    }

    suspend fun getSync(userWalletId: UserWalletId): List<WalletStoreModel> {
        return stores.first().getOrElse(userWalletId) { emptyList() }
    }

    private val mutex = Mutex()
    suspend fun update(
        f: suspend (HashMap<UserWalletId, List<WalletStoreModel>>) -> HashMap<UserWalletId, List<WalletStoreModel>>,
    ) {
        while (mutex.isLocked) {
            delay(timeMillis = 60)
        }

        mutex.withLock {
            val prevState = stores.first()
            stores.emit(f(prevState))
        }
    }
}
