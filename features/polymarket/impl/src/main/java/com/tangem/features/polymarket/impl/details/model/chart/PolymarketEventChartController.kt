package com.tangem.features.polymarket.impl.details.model.chart

import androidx.annotation.MainThread
import com.tangem.domain.polymarket.model.PolymarketLiveQuote
import com.tangem.domain.polymarket.model.PolymarketSoccerEvent
import com.tangem.features.polymarket.impl.common.probabilityUpdatedAtMillis
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketHistoryToken
import com.tangem.domain.polymarket.usecase.GetPolymarketPriceHistoryUseCase
import com.tangem.features.polymarket.impl.common.ui.chart.PolymarketProbabilityChart.Point
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.time.CurrentTimeProvider
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.time.Duration.Companion.minutes

@MainThread
internal class PolymarketEventChartController(
    private val scope: CoroutineScope,
    private val getHistory: GetPolymarketPriceHistoryUseCase,
    private val currentTimeProvider: CurrentTimeProvider,
    private val isEnabled: Boolean,
) {
    val state: StateFlow<PolymarketEventChartSession.Snapshot?>
        field = MutableStateFlow(null)

    private var session: PolymarketEventChartSession? = null
    private var source: Source? = null
    private var isResumed = false
    private var runningRequest: PolymarketEventChartSession.Request? = null
    private val loadJob = JobHolder()

    fun onResume() {
        isResumed = true
        session?.reload(currentTimeProvider.nowMillis())
        publishAndLoad()
    }

    fun onPause() {
        isResumed = false
        cancelLoading()
        state.value = session?.state?.value
    }

    fun update(event: PolymarketEvent) {
        if (!isEnabled || event.gameId != null || !event.teams.isNullOrEmpty()) return clear()
        update(Source.Event(event))
    }

    fun updateSoccer(
        event: PolymarketSoccerEvent,
        quotes: Map<String, PolymarketLiveQuote>,
        isRestUpdate: Boolean = true,
    ) {
        if (!isEnabled) return clear()
        val now = currentTimeProvider.nowMillis()
        val previous = (source as? Source.Soccer)?.takeIf { it.value.id == event.id }
        val timestamp = event.dataFetchedAt?.toEpochMilli()
            ?: if (isRestUpdate) now else previous?.restTimestampMillis
        val latestTimestamp = now + PolymarketEventChartSession.CLOCK_SKEW_TOLERANCE_MILLIS
        val liveQuotes = quotes.filterValues { quote ->
            event.isLive && event.isEnded != true &&
                quote.probabilityUpdatedAtMillis()?.let { it in 1..latestTimestamp } == true
        }
        update(Source.Soccer(event, liveQuotes, timestamp), retryFailed = isRestUpdate)
    }

    private fun update(source: Source, retryFailed: Boolean = true) {
        val now = currentTimeProvider.nowMillis()
        val previous = session?.state?.value?.content
        val content = when (source) {
            is Source.Event -> PolymarketChartContent.create(source.value, now, previous = previous)
            is Source.Soccer -> PolymarketSoccerChartContent.create(source.value, source.quotes)
        } ?: return clear()
        this.source = source
        if (previous?.eventId != content.eventId) {
            cancelLoading()
            session = PolymarketEventChartSession(content).apply { reload(now) }
        } else {
            session?.refresh(
                content,
                now,
                quoteTimestampsMillis = content.outcomes.associate { it.key to source.timestamp(it.key.assetId, now) },
            )
            if (retryFailed && session?.state?.value?.hasFailure == true) session?.reload(now)
        }
        if (!isResumed) cancelLoading()
        publishAndLoad()
    }

    private fun clear() {
        cancelLoading()
        session = null
        source = null
        state.value = null
    }

    fun onRefreshFailed() {
        val current = session ?: return
        current.refresh(
            current.state.value.content,
            currentTimeProvider.nowMillis(),
            quoteTimestampsMillis = emptyMap(),
        )
        publishAndLoad()
    }

    fun selectPeriod(id: String) {
        if (!isResumed) return
        val period = PolymarketChartPeriod.entries.firstOrNull { it.id == id } ?: return
        session?.selectPeriod(period, currentTimeProvider.nowMillis())
        publishAndLoad()
    }

    private fun cancelLoading() {
        loadJob.cancel()
        runningRequest = null
        session?.cancelLoading(currentTimeProvider.nowMillis())
    }

    private fun publishAndLoad() {
        val current = session ?: return
        val snapshot = current.state.value
        state.value = snapshot
        val request = snapshot.request
        if (request == null) {
            loadJob.cancel()
            runningRequest = null
            return
        }
        val loadedSource = source ?: return
        if (!isResumed || request === runningRequest) return
        loadJob.cancel()
        runningRequest = request
        val query = request.period.interval.historyQuery(
            lifetime = snapshot.content.lifetime,
            now = Instant.ofEpochMilli(currentTimeProvider.nowMillis()),
        )
        scope.launch {
            val tokens = request.outcomes.associate { outcome ->
                val key = outcome.key
                PolymarketHistoryToken(key.marketId, key.assetId, key.version) to key
            }
            val results = when (loadedSource) {
                is Source.Event -> getHistory(loadedSource.value, tokens.keys, query)
                is Source.Soccer -> getHistory.getSoccerHistory(loadedSource.value, tokens.keys, query)
            }
            ensureActive()
            val history = PolymarketChartHistory(
                points = results.mapNotNull { (token, result) ->
                    result.getOrNull()?.let { points ->
                        tokens.getValue(token) to points.map { Point(it.timestampMillis, it.probability.toDouble()) }
                            .toImmutableList()
                    }
                }.toMap().toImmutableMap(),
                fidelity = query.fidelityMinutes.minutes,
            )
            current.apply(request, history, currentTimeProvider.nowMillis())
            if (session === current) state.value = current.state.value
        }.saveIn(loadJob)
    }

    private sealed interface Source {
        data class Event(val value: PolymarketEvent) : Source
        data class Soccer(
            val value: PolymarketSoccerEvent,
            val quotes: Map<String, PolymarketLiveQuote>,
            val restTimestampMillis: Long?,
        ) : Source

        fun timestamp(assetId: String, now: Long): Long? = when (this) {
            is Event -> value.dataFetchedAt?.toEpochMilli() ?: now
            is Soccer -> if (value.isEnded == true) {
                null
            } else {
                quotes[assetId].probabilityUpdatedAtMillis() ?: restTimestampMillis
            }
        }
    }
}