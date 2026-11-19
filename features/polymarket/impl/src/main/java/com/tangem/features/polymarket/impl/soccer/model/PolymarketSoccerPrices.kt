package com.tangem.features.polymarket.impl.soccer.model

import androidx.annotation.MainThread
import com.tangem.domain.polymarket.model.PolymarketLiveQuote
import com.tangem.domain.polymarket.model.PolymarketSoccerEvent
import com.tangem.domain.polymarket.model.PredictionOrderSide
import com.tangem.domain.polymarket.usecase.GetPredictionPricesUseCase
import com.tangem.domain.polymarket.usecase.ObservePolymarketLiveQuotesUseCase
import com.tangem.features.polymarket.impl.common.buyPrice
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import java.math.BigDecimal
import kotlin.time.Duration.Companion.seconds

@MainThread
internal class PolymarketSoccerPrices(
    private val scope: CoroutineScope,
    private val getPrices: GetPredictionPricesUseCase,
    private val observeQuotes: ObservePolymarketLiveQuotesUseCase,
    private val isEnabled: Boolean,
    private val onStale: () -> Unit,
    private val onQuoteRecovery: () -> Unit,
) {
    val state: StateFlow<Map<String, BigDecimal?>>
        field = MutableStateFlow(emptyMap())

    val liveQuotes: StateFlow<Map<String, PolymarketLiveQuote>>
        field = MutableStateFlow(emptyMap())

    private var plan: PolymarketSoccerPricePlan? = null
    private var rest = emptyMap<String, BigDecimal?>()
    private var batch = emptyMap<String, BigDecimal?>()
    private var quotes = emptyMap<String, PolymarketLiveQuote>()
    private var finalPrices: Map<String, BigDecimal?>? = null
    private var isAwaitingFinalSnapshot = false
    private var isResumed = false
    private var isStaleReported = false
    private val pollJob = JobHolder()
    private val quoteJob = JobHolder()

    fun update(event: PolymarketSoccerEvent, hasReopened: Boolean = false) {
        if (!isEnabled) return
        val wasAwaitingFinalSnapshot = isAwaitingFinalSnapshot
        if (hasReopened) isAwaitingFinalSnapshot = false
        val previous = plan
        val hasChangedEvent = previous?.eventId != event.id
        if (hasChangedEvent) {
            isAwaitingFinalSnapshot = false
            batch = emptyMap()
            finalPrices = null
        }
        if (isAwaitingFinalSnapshot && event.isEnded != true) return
        isAwaitingFinalSnapshot = false
        val next = planFor(event)
        rest = event.buttonPrices()
        if (hasChangedEvent || !isResumed) quotes = emptyMap()
        batch = batch.filterKeys { it in next.polled }
        quotes = quotes.filterKeys { it in next.streamed }
        if (next.isEnded && finalPrices == null) finalPrices = rest
        plan = next
        publish()
        if (isResumed && (previous != next || wasAwaitingFinalSnapshot)) restart()
    }

    private fun planFor(event: PolymarketSoccerEvent): PolymarketSoccerPricePlan {
        val previous = plan
        return if (previous?.eventId == event.id && finalPrices != null) {
            previous
        } else {
            PolymarketSoccerPricePlan.create(event)
        }
    }

    /** Resume only after a fresh structural response has established the current match phase. */
    fun onResume() {
        if (!isEnabled || isResumed) return
        isResumed = true
        restart()
    }

    fun onPause() {
        isResumed = false
        pollJob.cancel()
        quoteJob.cancel()
    }

    fun awaitFinalSnapshot() {
        isAwaitingFinalSnapshot = true
        pollJob.cancel()
        quoteJob.cancel()
    }

    private fun restart() {
        pollJob.cancel()
        quoteJob.cancel()
        val current = plan ?: return
        if (current.isEnded || isAwaitingFinalSnapshot) return
        if (current.polled.isNotEmpty()) startPolling(current)
        if (current.streamed.isNotEmpty()) startQuotes(current)
    }

    private fun startPolling(current: PolymarketSoccerPricePlan) {
        scope.launch {
            while (true) {
                // CLOB's SELL side is the resting ask consumed by a purchase, as in the existing order flow.
                val result = getPrices(current.polled.toList(), PredictionOrderSide.SELL)
                ensureActive()
                if (plan != current || !isResumed) return@launch
                result.fold(
                    ifLeft = { error ->
                        TangemLogger.e("Soccer batch prices failed: $error")
                        if (!isStaleReported) {
                            isStaleReported = true
                            onStale()
                        }
                    },
                    ifRight = { prices ->
                        batch = batch + prices.filterKeys { it in current.polled }
                        isStaleReported = false
                        publish()
                    },
                )
                delay(POLL_INTERVAL)
            }
        }.saveIn(pollJob)
    }

    @OptIn(FlowPreview::class)
    private fun startQuotes(current: PolymarketSoccerPricePlan) {
        scope.launch {
            var wasLive = false
            var hasBeenLive = false
            observeQuotes(current.streamed).sample(QUOTE_INTERVAL_MILLIS).collect { update ->
                ensureActive()
                if (plan != current || !isResumed) return@collect
                quotes = update.filterKeys { it in current.streamed }
                val hasLiveQuotes = quotes.values.any { it.status == PolymarketLiveQuote.Status.LIVE }
                publish()
                if (hasLiveQuotes && hasBeenLive && !wasLive) onQuoteRecovery()
                hasBeenLive = hasBeenLive || hasLiveQuotes
                wasLive = hasLiveQuotes
            }
        }.saveIn(quoteJob)
    }

    private fun publish() {
        liveQuotes.value = if (finalPrices != null) emptyMap() else quotes
        state.value = finalPrices ?: rest + batch + quotes.mapValues { (assetId, quote) ->
            quote.buyPrice(rest[assetId])
        }
    }

    private companion object {
        val POLL_INTERVAL = 10.seconds
        const val QUOTE_INTERVAL_MILLIS = 300L
    }
}