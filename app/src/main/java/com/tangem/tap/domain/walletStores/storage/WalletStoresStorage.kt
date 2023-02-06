package com.tangem.tap.domain.walletStores.storage

import com.tangem.domain.common.util.UserWalletId
import com.tangem.tap.domain.model.WalletStoreModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal object WalletStoresStorage {
    private val stores = MutableSharedFlow<HashMap<UserWalletId, List<WalletStoreModel>>>(replay = 1)
    private val mutex = Mutex()

    init {
        stores.tryEmit(hashMapOf())
    }

    fun getAll(): SharedFlow<Map<UserWalletId, List<WalletStoreModel>>> {
        return stores.asSharedFlow()
    }

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
