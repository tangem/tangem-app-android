package com.tangem.features.polymarket.impl.common.ui.chart

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import com.google.common.truth.Truth.assertThat
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.ChartValues
import com.patrykandpatrick.vico.core.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.core.common.data.MutableExtraStore
import com.tangem.features.polymarket.impl.common.ui.chart.PolymarketProbabilityChart.Point
import com.tangem.features.polymarket.impl.common.ui.chart.PolymarketProbabilityChart.Series
import com.tangem.features.polymarket.impl.common.ui.chart.PolymarketProbabilityChart.SeriesColor
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicBoolean

internal class ProbabilityChartUpdatesTest {

    @Test
    fun `GIVEN commit in progress WHEN newer snapshots arrive THEN commit finishes and latest data is applied`() = runTest {
        withContext(Dispatchers.Default) {
            withTimeout(5_000) {
                // Arrange
                val producer = CartesianChartModelProducer()
                val series = mutableStateOf(createSeries())
                val commitEntered = CompletableDeferred<Unit>()
                val releaseCommit = CompletableDeferred<Unit>()
                val wasCancelled = CompletableDeferred<Unit>()
                val shouldBlock = AtomicBoolean(false)
                val committed = Channel<Double>(Channel.UNLIMITED)
                producer.registerForUpdates(
                    key = this,
                    cancelAnimation = {
                        if (shouldBlock.compareAndSet(true, false)) {
                            commitEntered.complete(Unit)
                            try {
                                releaseCommit.await()
                            } catch (error: CancellationException) {
                                wasCancelled.complete(Unit)
                                throw error
                            }
                        }
                    },
                    startAnimation = {},
                    prepareForTransformation = { model, _, _ ->
                        val line = model?.models?.firstOrNull() as? LineCartesianLayerModel
                        line?.let { committed.trySend(it.series.single().last().y) }
                    },
                    transform = { _, _ -> },
                    extraStore = MutableExtraStore(),
                    updateChartValues = { ChartValues.Empty },
                    onModelCreated = { _, _ -> },
                )
                shouldBlock.set(true)
                val updates = launch { producer.collectProbabilitySeries(series) }
                try {
                    // Act
                    commitEntered.await()
                    Snapshot.withMutableSnapshot { series.value = createSeries(probability = 0.5) }
                    Snapshot.withMutableSnapshot { series.value = createSeries(probability = 0.75) }
                    val cancelled = withTimeoutOrNull(100) { wasCancelled.await() }
                    releaseCommit.complete(Unit)
                    val values = mutableListOf(committed.receive(), committed.receive())
                    Snapshot.withMutableSnapshot { series.value = createSeries(probability = 0.875) }
                    values += committed.receive()

                    // Assert
                    assertThat(cancelled).isNull()
                    assertThat(values).containsExactly(0.25, 0.75, 0.875).inOrder()
                } finally {
                    releaseCommit.complete(Unit)
                    updates.cancelAndJoin()
                    producer.unregisterFromUpdates(this)
                    committed.close()
                }
            }
        }
    }

    private fun createSeries(probability: Double = 0.25): List<Series> = listOf(
        Series(
            id = "outcome",
            title = "Outcome",
            color = SeriesColor.Blue,
            points = persistentListOf(Point(0, 0.125), Point(1, probability)),
        ),
    )
}