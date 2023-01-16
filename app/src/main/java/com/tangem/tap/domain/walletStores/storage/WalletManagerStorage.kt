package com.tangem.tap.domain.walletStores.storage

import com.tangem.blockchain.common.WalletManager
import com.tangem.domain.common.util.UserWalletId
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal object WalletManagerStorage {
    private val managers = MutableSharedFlow<HashMap<UserWalletId, List<WalletManager>>>(replay = 1)
    private val mutex = Mutex()

    init {
        managers.tryEmit(hashMapOf())
    }

    fun getAll(): SharedFlow<Map<UserWalletId, List<WalletManager>>> {
        return managers.asSharedFlow()
    }

    suspend fun update(
        f: suspend (HashMap<UserWalletId, List<WalletManager>>) -> HashMap<UserWalletId, List<WalletManager>>,
    ) {
        while (mutex.isLocked) {
            delay(timeMillis = 60)
        }

        mutex.withLock {
            val prevState = managers.first()
            managers.emit(f(prevState))
        }
    }
}
