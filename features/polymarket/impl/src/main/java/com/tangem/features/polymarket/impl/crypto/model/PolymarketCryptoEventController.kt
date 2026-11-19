package com.tangem.features.polymarket.impl.crypto.model

import androidx.annotation.MainThread
import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketServerClock
import com.tangem.domain.polymarket.model.PolymarketCryptoPriceFeed
import com.tangem.domain.polymarket.model.PolymarketCryptoPricePoint
import com.tangem.domain.polymarket.model.PolymarketCryptoPriceSeries
import com.tangem.domain.polymarket.model.PolymarketCryptoWindow
import com.tangem.domain.polymarket.model.PolymarketCryptoWindowPhase
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketEventError
import com.tangem.domain.polymarket.model.PolymarketLiveQuote
import com.tangem.domain.polymarket.usecase.GetPolymarketEventUseCase
import com.tangem.domain.polymarket.usecase.ObservePolymarketCryptoPricesUseCase
import com.tangem.domain.polymarket.usecase.ObservePolymarketLiveQuotesUseCase
import com.tangem.features.polymarket.impl.common.secondTicks
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

/**
 * Keeps a crypto Up/Down event on screen: loads it, moves the clock on, reads the event again while its window is
 * open and around the window's bounds, switches windows by the user's choice, and follows the coin's price and the
 * outcomes' quotes of the shown window.
 *
 * Nothing runs while the screen is not in front: [onPause] stops every read and stream, [onResume] starts them
 * again with a fresh read. A window that has ended is not polled: only [PolymarketCryptoBoundaryRefresh] reads it,
 * and that gives up in the end.
 *
 * @param eventId event the screen was opened with; the shown one changes as windows are switched
 * @param onStaleData a read failed and the screen keeps what it has
 */
@MainThread
@Suppress("LongParameterList")
internal class PolymarketCryptoEventController(
    private val scope: CoroutineScope,
    private val userWalletId: UserWalletId,
    private val eventId: String,
    private val getEvent: GetPolymarketEventUseCase,
    private val observePrices: ObservePolymarketCryptoPricesUseCase,
    private val observeQuotes: ObservePolymarketLiveQuotesUseCase,
    private val clock: PolymarketServerClock,
    private val onStaleData: () -> Unit,
) {

    val state: StateFlow<PolymarketCryptoEventState>
        field = MutableStateFlow<PolymarketCryptoEventState>(PolymarketCryptoEventState.Loading)

    private var windows: PolymarketCryptoWindows? = null
    private var boundaryRefresh = PolymarketCryptoBoundaryRefresh(lastReadMillis = 0)
    private var points = emptyList<PolymarketCryptoPricePoint>()
    private var isPriceUnavailable = false
    private var quotes = emptyMap<String, PolymarketLiveQuote>()
    private var streamedFeed: PolymarketCryptoPriceFeed? = null
    private var streamedAssetIds = emptySet<String>()
    private var isResumed = false
    private var isStaleDataReported = false

    /** The refresh under way. A cancelled one leaves late, so it is told from the one that followed it. */
    private var runningRefresh: Any? = null

    private val loadJob = JobHolder()
    private val tickJob = JobHolder()
    private val pollJob = JobHolder()
    private val switchJob = JobHolder()
    private val priceJob = JobHolder()
    private val quoteJob = JobHolder()

    /** Reads the event the screen was opened with; also the retry after [PolymarketCryptoEventState.Failed]. */
    fun load() {
        stop()
        windows = null
        state.value = PolymarketCryptoEventState.Loading
        scope.launch {
            fetch(eventId).fold(
                ifLeft = { state.value = PolymarketCryptoEventState.Failed },
                ifRight = ::show,
            )
        }.saveIn(loadJob)
    }

    fun onResume() {
        if (isResumed) return
        isResumed = true
        if (windows == null) return
        boundaryRefresh = PolymarketCryptoBoundaryRefresh(lastReadMillis = clock.nowMillis())
        start(refreshNow = true)
    }

    fun onPause() {
        isResumed = false
        stop()
        windows = windows?.withoutSwitch()
        publish()
    }

    fun selectWindow(eventId: String) = choose { it.select(eventId = eventId, nowMillis = clock.nowMillis()) }

    fun goToLive() = choose { it.goToLive(clock.nowMillis()) }

    private fun show(event: PolymarketEvent) {
        val loaded = PolymarketCryptoWindows.of(event)
        if (loaded == null) {
            state.value = PolymarketCryptoEventState.NotUpDown
            return
        }
        windows = loaded
        boundaryRefresh = PolymarketCryptoBoundaryRefresh(lastReadMillis = clock.nowMillis())
        isStaleDataReported = false
        isPriceUnavailable = loaded.window.priceFeed == null
        publish()
        if (isResumed) start(refreshNow = false)
    }

    private fun start(refreshNow: Boolean) {
        scope.launch { clock.secondTicks().collect { onTick(it) } }.saveIn(tickJob)
        startPolling(refreshNow)
        followPrices()
        followQuotes()
    }

    private fun stop() {
        tickJob.cancel()
        pollJob.cancel()
        runningRefresh = null
        switchJob.cancel()
        priceJob.cancel()
        quoteJob.cancel()
    }

    private fun onTick(nowMillis: Long) {
        val shown = windows?.shown ?: return
        val claimed = boundaryRefresh.claim(event = shown, nowMillis = nowMillis)
        if (claimed != null) {
            boundaryRefresh = claimed
            // A read under way is left to finish: restarting it on every claim would starve a slow one
            if (runningRefresh == null) startPolling(refreshNow = true)
        }
        followPrices()
        followQuotes()
        publish()
    }

    private fun startPolling(refreshNow: Boolean) {
        runningRefresh = null
        scope.launch {
            if (refreshNow) refresh()
            while (windows?.phaseAt(clock.nowMillis()) != PolymarketCryptoWindowPhase.ENDED) {
                delay(POLL_INTERVAL)
                refresh()
            }
        }.saveIn(pollJob)
    }

    private suspend fun refresh() {
        val shownId = windows?.shown?.id ?: return
        val startedAtMillis = clock.nowMillis()
        val refresh = Any()
        runningRefresh = refresh
        val result = try {
            fetch(shownId)
        } finally {
            if (runningRefresh === refresh) runningRefresh = null
        }
        result.fold(
            ifLeft = { error ->
                TangemLogger.e("Crypto event: refresh of $shownId failed, keeping the last data: $error")
                if (!isStaleDataReported) {
                    isStaleDataReported = true
                    onStaleData()
                }
            },
            ifRight = { event ->
                isStaleDataReported = false
                receive(event, startedAtMillis)
            },
        )
    }

    private fun choose(choice: (PolymarketCryptoWindows) -> PolymarketCryptoWindows) {
        val previous = windows?.takeIf { isResumed } ?: return
        val chosen = choice(previous)
        if (chosen.switchingTo == previous.switchingTo) return
        windows = chosen
        publish()
        switchJob.cancel()
        val target = chosen.switchingTo ?: return
        scope.launch { switchTo(target, mayRedirectToLive = chosen.isGoingToLive) }.saveIn(switchJob)
    }

    private suspend fun switchTo(target: String, mayRedirectToLive: Boolean) {
        val startedAtMillis = clock.nowMillis()
        val result = fetch(target)
        result.fold(
            ifLeft = { error ->
                TangemLogger.e("Crypto event: window $target failed to load, staying on the shown one: $error")
                windows = windows?.failed(target)
                onStaleData()
                publish()
            },
            ifRight = { event ->
                isStaleDataReported = false
                receive(event, startedAtMillis)
            },
        )
        // The backend's word on which window is live ages with the answer that carried it, so it can lead to a
        // window that has ended; that window's own answer names the live one
        val landed = windows?.takeIf { mayRedirectToLive && result.isRight() && it.shown.id == target } ?: return
        val nowMillis = clock.nowMillis()
        if (landed.phaseAt(nowMillis) != PolymarketCryptoWindowPhase.ENDED) return
        val redirected = landed.goToLive(nowMillis)
        val liveTarget = redirected.switchingTo ?: return
        windows = redirected
        publish()
        switchTo(liveTarget, mayRedirectToLive = false)
    }

    private fun receive(event: PolymarketEvent, startedAtMillis: Long) {
        val previous = windows ?: return
        val received = previous.received(event)
        if (received === previous) return
        windows = received
        if (received.shown.id != previous.shown.id) onWindowChanged(previous.window, startedAtMillis)
        followPrices()
        followQuotes()
        publish()
    }

    private fun onWindowChanged(previous: PolymarketCryptoWindow, startedAtMillis: Long) {
        val window = windows?.window ?: return
        boundaryRefresh = PolymarketCryptoBoundaryRefresh(lastReadMillis = startedAtMillis)
        // The chart goes on across the windows of one feed for as long as its stream does
        val isSameChart = window.priceFeed == previous.priceFeed && priceJob.isActive &&
            window.phaseAt(clock.nowMillis()) != PolymarketCryptoWindowPhase.ENDED
        if (!isSameChart) {
            priceJob.cancel()
            points = emptyList()
            isPriceUnavailable = false
        }
        startPolling(refreshNow = false)
    }

    private fun followPrices() {
        val window = windows?.window ?: return
        val feed = window.priceFeed
        when {
            feed == null -> {
                priceJob.cancel()
                streamedFeed = null
                points = emptyList()
                isPriceUnavailable = true
            }
            clock.nowMillis() >= window.end.toEpochMilli() + CLOSING_GRACE_MILLIS -> priceJob.cancel()
            feed != streamedFeed || !priceJob.isActive -> {
                if (feed != streamedFeed) {
                    points = emptyList()
                    isPriceUnavailable = false
                }
                streamedFeed = feed
                scope.launch { observePrices(userWalletId, feed).collect(::onPrices) }.saveIn(priceJob)
            }
        }
    }

    private fun onPrices(series: PolymarketCryptoPriceSeries) {
        // What is on screen stays until the stream has something to say
        if (series.status == PolymarketCryptoPriceSeries.Status.WAITING) return
        val endMillis = windows?.window?.end?.toEpochMilli() ?: return
        val fresh = series.points.filter { it.timestampMillis <= endMillis }
        val first = fresh.firstOrNull()
        if (first != null) {
            // A collector that joins a running stream gets ticks only until the snapshot it asks for comes, so the
            // chart on screen stays behind them, as far back as a series reaches
            val earliestKeptMillis = fresh.last().timestampMillis - PRICE_HISTORY_MILLIS
            points = points.filter { it.timestampMillis in earliestKeptMillis until first.timestampMillis } + fresh
        }
        isPriceUnavailable = series.status == PolymarketCryptoPriceSeries.Status.UNAVAILABLE
        publish()
    }

    @OptIn(FlowPreview::class)
    private fun followQuotes() {
        val shown = windows ?: return
        val market = shown.shown.markets.firstOrNull()
        val assetIds = if (shown.phaseAt(clock.nowMillis()) == PolymarketCryptoWindowPhase.ENDED) {
            emptySet()
        } else {
            market?.outcomes.orEmpty().mapNotNullTo(HashSet()) { it.assetId.takeIf(String::isNotBlank) }
        }
        if (assetIds == streamedAssetIds && (assetIds.isEmpty() || quoteJob.isActive)) return
        quoteJob.cancel()
        streamedAssetIds = assetIds
        quotes = emptyMap()
        if (assetIds.isEmpty()) return
        scope.launch {
            observeQuotes(assetIds).sample(QUOTE_INTERVAL_MILLIS).collect { update ->
                quotes = update
                publish()
            }
        }.saveIn(quoteJob)
    }

    private suspend fun fetch(eventId: String): Either<PolymarketEventError, PolymarketEvent> {
        val first = getEvent(userWalletId = userWalletId, eventId = eventId)
        if (first.isRight()) return first

        delay(RETRY_DELAY)
        return getEvent(userWalletId = userWalletId, eventId = eventId)
    }

    private fun publish() {
        val shown = windows ?: return
        state.value = PolymarketCryptoEventState.Content(
            windows = shown,
            nowMillis = clock.nowMillis(),
            points = points,
            isPriceUnavailable = isPriceUnavailable,
            quotes = quotes,
        )
    }

    private companion object {
        val POLL_INTERVAL = 10.seconds
        val RETRY_DELAY = 2.seconds

        /** The stream outlives the window by this long: the chart draws a little behind the clock. */
        const val CLOSING_GRACE_MILLIS = 5_000L

        /** How far behind its newest point a price series reaches. */
        const val PRICE_HISTORY_MILLIS = 127_500L
        const val QUOTE_INTERVAL_MILLIS = 300L
    }
}