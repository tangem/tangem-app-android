package com.tangem.domain.jointaccount.polling

import com.tangem.domain.jointaccount.fetcher.SingleJointAccountListFetcher
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Polls joint-account state for a wallet every [POLL_INTERVAL] while at least one subscriber collects
 * [poll]. One self-paced ticker per wallet, paced off the wall-clock time of its own previous fetch — never
 * reset or fast-forwarded by an outside event (a pull-to-refresh, a mutating use case).
 *
 * A wallet's very first fetch waits out a full [POLL_INTERVAL] too, same as every one after it — [poll] never
 * fires on subscribe. Every current caller starts collecting [poll] alongside its own explicit initial fetch
 * (a screen's `refreshAccounts()`, an already-resolved account elsewhere), so an immediate first fetch here
 * would race that unserialized write to the same wallet's cache; waiting it out leaves that first fetch solely
 * to the caller.
 *
 * The ticker's underlying producer coroutine can still be torn down by [SharingStarted.WhileSubscribed] once
 * every subscriber is gone for [STOP_TIMEOUT_MILLIS] — that's fine: [lastFetchAt] lives outside that
 * coroutine, so a restart doesn't lose the wait. Coming back mid-interval waits out only what's left of it;
 * coming back once the interval has already fully elapsed fetches right away, same as a brand-new ticker.
 *
 * [fetch] itself never throws — a failed request surfaces as [arrow.core.Either.Left] and is only logged — but
 * should the producer coroutine still die for some other reason, it retries after [POLL_INTERVAL] rather than
 * leaving the wallet's ticker dead in [tickers] for the rest of the process.
 */
class JointAccountPollingCoordinator(
    private val fetcher: SingleJointAccountListFetcher,
    private val appScope: AppCoroutineScope,
    private val now: () -> Long = System::currentTimeMillis,
) {

    private val tickers = ConcurrentHashMap<UserWalletId, Flow<Unit>>()
    private val lastFetchAt = ConcurrentHashMap<UserWalletId, Long>()

    fun poll(walletId: UserWalletId): Flow<Unit> = tickers.computeIfAbsent(walletId, ::createTicker)

    private fun createTicker(walletId: UserWalletId): Flow<Unit> = flow {
        while (currentCoroutineContext().isActive) {
            awaitNextFetch(walletId)
            fetch(walletId)
            emit(Unit)
        }
    }.retryWhen { cause, _ ->
        TangemLogger.e("Joint account polling ticker crashed for wallet $walletId, retrying", cause)
        delay(POLL_INTERVAL)
        true
    }.shareIn(
        scope = appScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = STOP_TIMEOUT_MILLIS),
        replay = 0,
    )
    private suspend fun awaitNextFetch(walletId: UserWalletId) {
        val elapsedSinceLastFetch = lastFetchAt[walletId]?.let { now() - it } ?: 0L
        val remaining = POLL_INTERVAL.inWholeMilliseconds - elapsedSinceLastFetch
        if (remaining > 0) delay(remaining.milliseconds)
    }

    private suspend fun fetch(walletId: UserWalletId) {
        fetcher(walletId).onLeft { error ->
            TangemLogger.e("Failed to poll joint account state for wallet $walletId", error)
        }
        lastFetchAt[walletId] = now()
    }

    private companion object {
        val POLL_INTERVAL = 60.seconds
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}