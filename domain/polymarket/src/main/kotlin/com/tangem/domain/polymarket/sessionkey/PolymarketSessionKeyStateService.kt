package com.tangem.domain.polymarket.sessionkey

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.interactor.PolymarketSessionKeyOverview
import com.tangem.domain.polymarket.interactor.ReconcilePolymarketSessionKeysInteractor
import com.tangem.domain.polymarket.model.PolymarketWalletError
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Clock
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private typealias SessionKeyOverviewResult = Either<PolymarketWalletError, PolymarketSessionKeyOverview>

class PolymarketSessionKeyStateService(
    private val reconcile: ReconcilePolymarketSessionKeysInteractor,
    private val scope: CoroutineScope,
    private val clock: Clock = Clock.systemUTC(),
) {

    private val wallets = ConcurrentHashMap<UserWalletId, WalletState>()

    fun observe(userWalletId: UserWalletId): Flow<SessionKeyOverviewResult> =
        wallets.computeIfAbsent(userWalletId) { WalletState(userWalletId) }.overview

    fun refresh(userWalletId: UserWalletId) {
        val refreshes = wallets[userWalletId]?.refreshes
        if (refreshes != null && refreshes.subscriptionCount.value > 0) {
            refreshes.tryEmit(Unit)
            return
        }
        scope.launch {
            reconcile(userWalletId).onLeft { error ->
                TangemLogger.e("Polymarket session keys of an unobserved wallet could not be reconciled: $error")
            }
        }
    }

    suspend fun refreshAndAwait(userWalletId: UserWalletId, timeout: Duration = REFRESH_AWAIT_TIMEOUT) {
        val wallet = wallets[userWalletId]
        if (wallet == null || wallet.refreshes.subscriptionCount.value == 0) {
            refresh(userWalletId)
            return
        }
        val nextOverview = withTimeoutOrNull(timeout) {
            coroutineScope {
                val awaited = async(start = CoroutineStart.UNDISPATCHED) {
                    wallet.overview.drop(wallet.overview.replayCache.size).first()
                }
                wallet.refreshes.tryEmit(Unit)
                awaited.await()
            }
        }
        if (nextOverview == null) TangemLogger.i("Polymarket session keys not refreshed within $timeout, moving on")
    }

    private inner class WalletState(userWalletId: UserWalletId) {

        val refreshes = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

        val overview: SharedFlow<SessionKeyOverviewResult> = channelFlow {
            val cycle = ReconcileCycle()
            val requested = Channel<Unit>(Channel.CONFLATED)
            launch(start = CoroutineStart.UNDISPATCHED) {
                refreshes.collect { requested.send(Unit) }
            }
            reconcileLoop(userWalletId, requested, cycle)
                .retryWhen { cause, _ ->
                    if (cause is CancellationException && !currentCoroutineContext().isActive) throw cause
                    val restart = cycle.takeRestart()
                    if (restart == 0) TangemLogger.e("Polymarket session key state loop failed, restarting it", cause)
                    delay(backoffDelay(restart))
                    true
                }
                .collect { send(it) }
        }
            .distinctUntilChanged()
            .shareIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(
                    stopTimeoutMillis = STOP_TIMEOUT.inWholeMilliseconds,
                    replayExpirationMillis = 0,
                ),
                replay = 1,
            )
    }

    private fun reconcileLoop(
        userWalletId: UserWalletId,
        requested: Channel<Unit>,
        cycle: ReconcileCycle,
    ): Flow<SessionKeyOverviewResult> = flow {
        while (true) {
            val overview = reconcile(userWalletId)
            if (cycle.accept(overview, now = clock.instant())) emit(overview)
            val isPolling = cycle.startWait()
            val isRefreshed = awaitRefresh(requested, timeout = idleTimeout(cycle, isPolling))
            cycle.endWait(isRefreshed = isRefreshed)
        }
    }

    private suspend fun awaitRefresh(requested: Channel<Unit>, timeout: Duration?): Boolean {
        if (timeout == null) {
            requested.receive()
            return true
        }
        return withTimeoutOrNull(timeout) { requested.receive() } != null
    }

    private fun idleTimeout(cycle: ReconcileCycle, isPolling: Boolean): Duration? {
        if (isPolling) return PolymarketSessionKeyRelay.POLL_INTERVAL
        val retry = cycle.takeElapsedChangeRetry() ?: return timeUntil(cycle.nextChangeAt)
        return backoffDelay(retry)
    }

    private fun timeUntil(moment: Instant?): Duration? {
        moment ?: return null
        val now = clock.instant()
        return (moment.toEpochMilli() - now.toEpochMilli()).milliseconds.coerceAtLeast(Duration.ZERO)
    }

    private fun backoffDelay(attempt: Int): Duration {
        val doublings = attempt.coerceAtMost(MAX_DOUBLINGS)
        return (PolymarketSessionKeyRelay.POLL_INTERVAL * (1 shl doublings)).coerceAtMost(MAX_BACKOFF)
    }

    private class ReconcileCycle {

        var nextChangeAt: Instant? = null
            private set

        private var hasLoaded = false
        private var isSettling = false
        private var isElapsedChangeFailure = false
        private var isFailureLogged = false
        private var polls = 0
        private var elapsedChangeRetries = 0
        private var restarts = 0

        fun accept(overview: SessionKeyOverviewResult, now: Instant): Boolean = overview.fold(
            ifLeft = { error ->
                if (nextChangeAt?.let { it <= now } == true) isElapsedChangeFailure = true
                nextChangeAt = nextChangeAt?.takeIf { it > now }
                if (hasLoaded && !isFailureLogged) {
                    TangemLogger.e("Polymarket session keys could not be refreshed, keeping the last state: $error")
                    isFailureLogged = true
                }
                !hasLoaded
            },
            ifRight = { loaded ->
                hasLoaded = true
                isElapsedChangeFailure = false
                isFailureLogged = false
                elapsedChangeRetries = 0
                restarts = 0
                isSettling = loaded.wallet.sessionKeys.any { key -> key.status.isSettling() }
                nextChangeAt = loaded.assessment.nextChangeAt
                true
            },
        )

        fun startWait(): Boolean {
            if (!isSettling) polls = 0
            val isPolling = isSettling && polls < SETTLE_REFRESHES
            if (isSettling && !isPolling) {
                TangemLogger.i("Polymarket session keys still settling after $polls polls, waiting for a refresh")
            }
            return isPolling
        }

        fun takeElapsedChangeRetry(): Int? {
            if (!isElapsedChangeFailure) return null
            return elapsedChangeRetries++
        }

        fun takeRestart(): Int = restarts++

        fun endWait(isRefreshed: Boolean) {
            polls = if (isRefreshed) 0 else polls + 1
        }
    }

    private companion object {
        val STOP_TIMEOUT: Duration = 5.seconds
        val REFRESH_AWAIT_TIMEOUT: Duration = 5.seconds
        const val SETTLE_REFRESHES = 100
        val MAX_BACKOFF: Duration = 1.minutes
        const val MAX_DOUBLINGS = 5
    }
}