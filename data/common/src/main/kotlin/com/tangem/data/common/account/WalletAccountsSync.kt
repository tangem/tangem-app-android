package com.tangem.data.common.account

import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * Serializes reads, writes and local edits of a wallet's accounts document: interleaved, they leave the stored ETag
 * next to a copy it does not describe, and every later conditional read keeps that copy with a `304`.
 */
@Singleton
class WalletAccountsSync @Inject constructor() {

    private val locks = ConcurrentHashMap<UserWalletId, Mutex>()
    private val walletsAheadOfBackend: MutableSet<UserWalletId> = ConcurrentHashMap.newKeySet()

    // Reentrant: a fetch pushes, and a push may fetch its ETag
    suspend fun <T> withLock(userWalletId: UserWalletId, block: suspend () -> T): T {
        val heldLocks = currentCoroutineContext()[HeldLocks]?.walletIds.orEmpty()
        if (userWalletId in heldLocks) return block()

        return locks.computeIfAbsent(userWalletId) { Mutex() }.withLock {
            withContext(HeldLocks(walletIds = heldLocks + userWalletId)) { block() }
        }
    }

    fun isLocalAhead(userWalletId: UserWalletId): Boolean = userWalletId in walletsAheadOfBackend

    fun markLocalAhead(userWalletId: UserWalletId) {
        walletsAheadOfBackend += userWalletId
    }

    fun markInSync(userWalletId: UserWalletId) {
        walletsAheadOfBackend -= userWalletId
    }

    private class HeldLocks(val walletIds: Set<UserWalletId>) : AbstractCoroutineContextElement(Key) {
        companion object Key : CoroutineContext.Key<HeldLocks>
    }
}