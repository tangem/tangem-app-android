package com.tangem.domain.polymarket

import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

class PolymarketDepositWalletLock {

    private val mutexes = ConcurrentHashMap<UserWalletId, Mutex>()

    /** @param action must not take the lock of the same wallet again: the lock is not reentrant */
    suspend fun <T> withLock(userWalletId: UserWalletId, action: suspend () -> T): T =
        mutexes.getOrPut(userWalletId) { Mutex() }.withLock { action() }
}