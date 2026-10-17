package com.tangem.domain.polymarket

import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Serialises, per wallet, every use of the stored CLOB credentials with every write of new ones.
 * Not reentrant: [action] must not take the lock of the same wallet again.
 */
class PolymarketCredentialsLock {

    private val mutexes = ConcurrentHashMap<UserWalletId, Mutex>()

    suspend fun <T> withLock(userWalletId: UserWalletId, action: suspend () -> T): T =
        mutexes.getOrPut(userWalletId) { Mutex() }.withLock { action() }
}