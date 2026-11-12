package com.tangem.features.polymarket.impl.details.model.chart

import androidx.annotation.MainThread
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
    private var event: PolymarketEvent? = null
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
        if (!isEnabled || event.gameId != null || !event.teams.isNullOrEmpty()) {
            cancelLoading()
            session = null
            this.event = null
            state.value = null
            return
        }
        val now = currentTimeProvider.nowMillis()
        val previous = session?.state?.value?.content
        this.event = event
        val content = PolymarketChartContent.create(event, now, previous = previous)
        if (previous?.eventId != event.id) {
            cancelLoading()
            session = PolymarketEventChartSession(content).apply { reload(now) }
        } else {
            session?.refresh(content, now, quoteTimestampMillis = event.dataFetchedAt?.toEpochMilli() ?: now)
            if (session?.state?.value?.hasFailure == true) session?.reload(now)
        }
        if (!isResumed) cancelLoading()
        publishAndLoad()
    }

    fun onRefreshFailed() {
        val current = session ?: return
        current.refresh(current.state.value.content, currentTimeProvider.nowMillis(), quoteTimestampMillis = null)
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
        val loadedEvent = event ?: return
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
            val results = getHistory(loadedEvent, tokens.keys, query)
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
}