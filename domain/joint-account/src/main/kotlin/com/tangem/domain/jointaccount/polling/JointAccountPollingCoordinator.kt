package com.tangem.domain.jointaccount.polling

import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.jointaccount.fetcher.SingleJointAccountListFetcher
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
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
 * to the caller. A caller with no fetch of its own — the main screen on a wallet switch — asks for it through
 * `isFirstFetchImmediate`.
 *
 * The ticker's underlying producer coroutine can still be torn down by [SharingStarted.WhileSubscribed] once
 * every subscriber is gone for [STOP_TIMEOUT_MILLIS] — that's fine: [lastFetchAt] lives outside that
 * coroutine, so a restart doesn't lose the wait. Coming back mid-interval waits out only what's left of it;
 * coming back once the interval has already fully elapsed fetches right away, same as a brand-new ticker.
 *
 * A failed fetch doubles the wait before the next one, up to [MAX_POLL_INTERVAL], so a backend that is down is
 * not hit every [POLL_INTERVAL]; the first success brings the pace back. The streak of failures lives in the
 * producer coroutine, so it is forgotten once every subscriber has been gone for [STOP_TIMEOUT_MILLIS] — coming
 * back to the app asks again at the usual pace.
 *
 * [fetch] itself never throws — a failed request surfaces as [arrow.core.Either.Left] and is only logged — but
 * should the producer coroutine still die for some other reason, it retries after [POLL_INTERVAL] rather than
 * leaving the wallet's ticker dead in [tickers] for the rest of the process.
 *
 * @param isWalletLinked passed as a function: the use case behind it lives in a module that depends on this one
 */
@OptIn(ExperimentalCoroutinesApi::class)
class JointAccountPollingCoordinator(
    private val fetcher: SingleJointAccountListFetcher,
    private val isWalletLinked: (UserWalletId) -> Flow<Boolean>,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val appScope: AppCoroutineScope,
    private val now: () -> Long = System::currentTimeMillis,
) {

    private val tickers = ConcurrentHashMap<UserWalletId, Flow<Unit>>()
    private val lastFetchAt = ConcurrentHashMap<UserWalletId, Long>()
    private val walletsFetchedOnFirstPoll = ConcurrentHashMap.newKeySet<UserWalletId>()

    /**
     * @param isFirstFetchImmediate fetch right away when the wallet has never been polled, instead of waiting out
     * the first interval. A wallet that has been polled before is paced by its last fetch either way
     */
    fun poll(walletId: UserWalletId, isFirstFetchImmediate: Boolean = false): Flow<Unit> {
        if (isFirstFetchImmediate) walletsFetchedOnFirstPoll += walletId

        return tickers.computeIfAbsent(walletId, ::createTicker)
    }

    /**
     * A forgotten wallet stops being polled at once, the request in flight included: the ticker outlives its
     * subscribers by [STOP_TIMEOUT_MILLIS], and an answer stored after the wallet's data is cleaned would bring
     * the joint accounts of a wallet that is gone back to the disk.
     */
    private fun createTicker(walletId: UserWalletId): Flow<Unit> =
        combine(isWalletLinked(walletId), isWalletStored(walletId)) { isLinked, isStored -> isLinked && isStored }
            .distinctUntilChanged()
            .flatMapLatest { isPollable -> if (isPollable) ticks(walletId) else emptyFlow() }
            .retryWhen { cause, _ ->
                TangemLogger.e("Joint account polling ticker crashed for wallet $walletId, retrying", cause)
                delay(POLL_INTERVAL)
                true
            }
            .shareIn(
                scope = appScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = STOP_TIMEOUT_MILLIS),
                replay = 0,
            )

    private fun isWalletStored(walletId: UserWalletId): Flow<Boolean> = userWalletsListRepository.userWallets
        .map { wallets -> wallets.orEmpty().any { it.walletId == walletId } }

    private fun ticks(walletId: UserWalletId): Flow<Unit> = flow {
        var failuresInRow = 0

        while (currentCoroutineContext().isActive) {
            awaitNextFetch(walletId = walletId, interval = pollInterval(failuresInRow))
            failuresInRow = if (fetch(walletId)) 0 else failuresInRow + 1
            emit(Unit)
        }
    }

    private fun pollInterval(failuresInRow: Int): Duration {
        val multiplier = 1 shl failuresInRow.coerceAtMost(MAX_BACKOFF_DOUBLINGS)

        return (POLL_INTERVAL * multiplier).coerceAtMost(MAX_POLL_INTERVAL)
    }

    private suspend fun awaitNextFetch(walletId: UserWalletId, interval: Duration) {
        val lastFetch = lastFetchAt[walletId]
        if (lastFetch == null && walletId in walletsFetchedOnFirstPoll) return

        val elapsedSinceLastFetch = lastFetch?.let { now() - it } ?: 0L
        val remaining = interval.inWholeMilliseconds - elapsedSinceLastFetch
        if (remaining > 0) delay(remaining.milliseconds)
    }

    /** @return `true` when the request succeeded */
    private suspend fun fetch(walletId: UserWalletId): Boolean {
        val result = fetcher(walletId).onLeft { error ->
            TangemLogger.e("Failed to poll joint account state for wallet $walletId", error)
        }
        lastFetchAt[walletId] = now()

        return result.isRight()
    }

    private companion object {
        val POLL_INTERVAL = 5.seconds
        val MAX_POLL_INTERVAL = 60.seconds
        const val MAX_BACKOFF_DOUBLINGS = 4
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}