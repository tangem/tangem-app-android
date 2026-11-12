package com.tangem.features.polymarket.impl.details.model.chart

import androidx.annotation.MainThread
import com.tangem.features.polymarket.impl.common.ui.chart.PolymarketProbabilityChart
import com.tangem.features.polymarket.impl.common.ui.chart.PolymarketProbabilityChart.LatestMarker
import com.tangem.features.polymarket.impl.common.ui.chart.PolymarketProbabilityChart.Point
import com.tangem.features.polymarket.impl.common.ui.chart.PolymarketProbabilityChart.Series
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration.Companion.hours

/**
 * One event's presentation session. The owner executes [Snapshot.request] and supplies normalized history via [apply].
 * Calls are serialized on the UI thread; every request has its own identity, including retries of the same period.
 */
@MainThread
class PolymarketEventChartSession(initialContent: PolymarketChartContent) {
    private var content = initialContent
    private val cache = mutableMapOf<PolymarketChartPeriod, Loaded>()
    private var displayed: Loaded? = null
    private var selectedPeriod = content.initialPeriod
    private var request: Request? = null
    private var lastLiveUpdate: Long? = null
    private var livePoints = emptyMap<PolymarketChartOutcome.Key, List<Point>>()
    private var hasFailure = false

    val state: StateFlow<Snapshot>
        field = MutableStateFlow(snapshot(nowMillis = 0))

    data class Snapshot(
        val content: PolymarketChartContent,
        val chart: PolymarketProbabilityChart.State,
        val selectedPeriod: PolymarketChartPeriod,
        val request: Request?,
        val hasFailure: Boolean,
    )

    class Request internal constructor(
        val eventId: String,
        val outcomes: ImmutableList<PolymarketChartOutcome>,
        val period: PolymarketChartPeriod,
    )

    fun selectPeriod(period: PolymarketChartPeriod, nowMillis: Long) {
        if (period !in content.periods || period == selectedPeriod) return
        preserveLivePoints(nowMillis)
        selectedPeriod = period
        cache[period]?.let { displayed = it }
        livePoints = emptyMap()
        lastLiveUpdate = null
        reload(nowMillis)
    }

    fun reload(nowMillis: Long) {
        request = content.outcomes.takeIf { it.isNotEmpty() }?.let {
            Request(content.eventId, it, selectedPeriod)
        }
        publish(nowMillis, hasFailure = false)
    }

    /** Null is a failed request; successful empty histories remain distinct from missing/failed series. */
    fun apply(completed: Request, history: PolymarketChartHistory?, nowMillis: Long) {
        if (request !== completed) return
        request = null
        if (history == null || history.points.keys.none { key -> content.outcomes.any { it.key == key } }) {
            selectedPeriod = displayed?.period ?: selectedPeriod
            publish(nowMillis, hasFailure = true)
            return
        }
        val loaded = Loaded(
            period = completed.period,
            series = content.series(history),
            fidelityMillis = history.fidelity.inWholeMilliseconds,
            loadedAt = nowMillis,
        )
        cache[completed.period] = loaded
        displayed = loaded
        livePoints = emptyMap()
        lastLiveUpdate = null
        publish(nowMillis, hasFailure = content.outcomes.any { it.key !in history.points })
    }

    fun refresh(updated: PolymarketChartContent, nowMillis: Long, quoteTimestampMillis: Long? = nowMillis) {
        val wasClosed = content.isClosed
        val isSameSelection = content.eventId == updated.eventId &&
            content.outcomes.map { it.key } == updated.outcomes.map { it.key } &&
            content.isComplementaryPair == updated.isComplementaryPair
        content = updated
        if (!isSameSelection) {
            cache.clear()
            displayed = null
            selectedPeriod = updated.initialPeriod
            livePoints = emptyMap()
            lastLiveUpdate = null
            reload(nowMillis)
            return
        }
        if (selectedPeriod !in content.periods) {
            displayed = displayed?.takeIf { it.period in content.periods }
            cache.keys.retainAll(content.periods)
            selectPeriod(content.initialPeriod, nowMillis)
            return
        }
        if (content.isClosed && !wasClosed) {
            preserveLivePoints(nowMillis)
            livePoints = emptyMap()
            reload(nowMillis)
            return
        }
        if (content.isClosed) livePoints = emptyMap()
        refreshLivePoints(nowMillis, quoteTimestampMillis)
        publish(nowMillis)
    }

    /** Called on pause/destroy by the owner alongside cancelling its load job. Cached content is retained. */
    fun cancelLoading(nowMillis: Long) {
        request = null
        selectedPeriod = displayed?.period ?: selectedPeriod
        publish(nowMillis)
    }

    private fun refreshLivePoints(nowMillis: Long, quoteTimestampMillis: Long?) {
        val loaded = displayed
        if (request == null && loaded != null && !content.isClosed) {
            val previousUpdate = if (streamsLive()) lastLiveUpdate ?: loaded.loadedAt else loaded.loadedAt
            if (nowMillis - previousUpdate >= loaded.fidelityMillis) {
                preserveLivePoints(nowMillis)
                livePoints = emptyMap()
                reload(nowMillis)
                return
            }
            quoteTimestampMillis?.takeIf { timestamp ->
                timestamp <= nowMillis && nowMillis - timestamp <= loaded.fidelityMillis * STALE_BUCKETS &&
                    timestamp > lastLiveUpdate ?: Long.MIN_VALUE
            }?.let(::recordLivePoints)
        }
    }

    private fun recordLivePoints(nowMillis: Long) {
        val updates = content.outcomes.mapNotNull { outcome ->
            val probability = outcome.probability?.toDouble()?.takeIf { it in 0.0..1.0 } ?: return@mapNotNull null
            outcome.key to Point(nowMillis, probability)
        }.toMap()
        if (updates.isEmpty()) return
        val recorded = livePoints.toMutableMap()
        updates.forEach { (key, point) ->
            val previous = livePoints[key].orEmpty().filter { it.timestampMillis < nowMillis }
            val points = if (streamsLive()) previous + point else listOf(point)
            val cutoff = nowMillis - LIVE_WINDOW.inWholeMilliseconds
            recorded[key] = points.filter { it.timestampMillis >= cutoff }
        }
        livePoints = recorded
        lastLiveUpdate = nowMillis
    }

    private fun streamsLive(): Boolean = content.isLive && selectedPeriod == PolymarketChartPeriod.Hour

    private fun preserveLivePoints(nowMillis: Long) {
        val loaded = displayed ?: return
        val preserved = loaded.copy(
            series = content.withLivePoints(
                series = loaded.series,
                livePoints = livePoints,
                period = loaded.period,
                nowMillis = nowMillis,
            ),
        )
        displayed = preserved
        cache[loaded.period] = preserved
    }

    private fun publish(nowMillis: Long, hasFailure: Boolean = this.hasFailure) {
        this.hasFailure = hasFailure
        state.value = snapshot(nowMillis, hasFailure)
    }

    private fun snapshot(nowMillis: Long, hasFailure: Boolean = false): Snapshot {
        val loaded = displayed
        val isAwaitingPeriod = request != null && (loaded == null || loaded.period != selectedPeriod)
        val chart = when {
            isAwaitingPeriod -> PolymarketProbabilityChart.State.Loading
            loaded == null -> PolymarketProbabilityChart.State.NoData
            else -> {
                val lines = content.withLivePoints(
                    series = loaded.series,
                    livePoints = livePoints,
                    period = loaded.period,
                    nowMillis = nowMillis,
                )
                val latest = lines.mapNotNull { it.points.lastOrNull()?.timestampMillis }.maxOrNull()
                val marker = when {
                    content.isClosed || latest == null || nowMillis - latest > loaded.fidelityMillis * STALE_BUCKETS ->
                        LatestMarker.Hidden
                    streamsLive() -> LatestMarker.Pulsing
                    else -> LatestMarker.Still
                }
                lines.asChartState(marker)
            }
        }
        return Snapshot(
            content = content,
            chart = chart,
            selectedPeriod = selectedPeriod,
            request = request,
            hasFailure = hasFailure,
        )
    }

    private data class Loaded(
        val period: PolymarketChartPeriod,
        val series: ImmutableList<Series>,
        val fidelityMillis: Long,
        val loadedAt: Long,
    )

    private companion object {
        const val STALE_BUCKETS = 2
        val LIVE_WINDOW = 1.hours
    }
}