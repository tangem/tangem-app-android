package com.tangem.domain.polymarket

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.sessionkey.PolymarketPendingAuthorizationStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.util.concurrent.ConcurrentHashMap

class PolymarketDepositWalletLock(
    private val pendingAuthorizations: PolymarketPendingAuthorizationStore,
    private val clock: Clock = Clock.systemUTC(),
) {

    private val holders = ConcurrentHashMap<UserWalletId, MutableStateFlow<Holder?>>()

    /** @param action must not take the lock of the same wallet again: the lock is not reentrant */
    suspend fun <T> withSessionKeyOperation(userWalletId: UserWalletId, action: suspend () -> T): T {
        val holder = holderOf(userWalletId)
        while (!holder.compareAndSet(expect = null, update = Holder.SESSION_KEY_OPERATION)) {
            holder.first { it == null }
        }
        return holder.releasedAfter(action)
    }

    suspend fun <T> withBatchOrBusy(userWalletId: UserWalletId, action: suspend () -> T): Either<Busy, T> {
        val holder = holderOf(userWalletId)
        while (!holder.compareAndSet(expect = null, update = Holder.BATCH)) {
            when (holder.value) {
                Holder.SESSION_KEY_OPERATION -> return Busy.left()
                Holder.BATCH -> holder.first { it != Holder.BATCH }
                null -> Unit
            }
        }
        return holder.releasedAfter {
            if (isAuthorizationPending(userWalletId)) Busy.left() else action().right()
        }
    }

    private suspend fun isAuthorizationPending(userWalletId: UserWalletId): Boolean =
        pendingAuthorizations.get(userWalletId).fold(
            ifLeft = { true },
            ifRight = { pending -> pending?.isInFlight(clock.millis()) == true },
        )

    private fun holderOf(userWalletId: UserWalletId): MutableStateFlow<Holder?> =
        holders.getOrPut(userWalletId) { MutableStateFlow(null) }

    private suspend fun <T> MutableStateFlow<Holder?>.releasedAfter(action: suspend () -> T): T = try {
        action()
    } finally {
        value = null
    }

    data object Busy

    private enum class Holder { SESSION_KEY_OPERATION, BATCH }
}