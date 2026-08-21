package com.tangem.tap.common.analytics.handlers.opentelemetry

import android.app.Application
import android.content.ComponentCallbacks2
import com.google.common.truth.Truth.assertThat
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.opentelemetry.contrib.disk.buffering.storage.SignalStorage
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.metrics.InstrumentType
import io.opentelemetry.sdk.metrics.data.AggregationTemporality
import io.opentelemetry.sdk.metrics.data.MetricData
import io.opentelemetry.sdk.metrics.export.MetricExporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class OpenTelemetryMetricsClientTest {

    private val dispatchers = TestingCoroutineDispatcherProvider()
    private val networkExporter = RecordingMetricExporter()

    private val componentCallbacks = slot<android.content.ComponentCallbacks>()

    private fun CoroutineScope.createClient(): OpenTelemetryMetricsClient {
        val application: Application = mockk(relaxed = true) {
            every { registerComponentCallbacks(capture(componentCallbacks)) } returns Unit
        }
        return OpenTelemetryMetricsClient(
            application = application,
            apiKey = "test-key",
            scope = this,
            dispatchers = dispatchers,
            networkExporter = networkExporter,
            storage = InMemoryMetricStorage(),
        ).apply { start(application) }
    }

    @Test
    fun `GIVEN recorded counter WHEN flush THEN batch is drained to the network exporter`() = runTest {
        // Arrange
        val client = createClient()
        client.getMeter().counterBuilder("app_test_event").build().add(1)

        // Act
        client.flush()

        // Assert
        val exported = networkExporter.exportedBatches.flatten()
        assertThat(exported.map { it.name }).contains("app_test_event")
    }

    @Test
    fun `GIVEN failed network export WHEN next flush THEN batch is redelivered from the WAL`() = runTest {
        // Arrange
        val client = createClient()
        networkExporter.shouldFail = true
        client.getMeter().counterBuilder("app_test_first").build().add(1)
        client.flush()

        // Act
        networkExporter.shouldFail = false
        client.getMeter().counterBuilder("app_test_second").build().add(1)
        client.flush()

        // Assert
        val delivered = networkExporter.successfulBatches.flatten().map { it.name }
        assertThat(delivered).containsAtLeast("app_test_first", "app_test_second")
    }

    @Test
    fun `GIVEN exported batch WHEN inspecting resource THEN only app coordinates are attached`() = runTest {
        // Arrange
        val client = createClient()
        client.getMeter().counterBuilder("app_test_event").build().add(1)

        // Act
        client.flush()

        // Assert
        val resource = networkExporter.exportedBatches.flatten().first().resource
        val attributeKeys = resource.attributes.asMap().keys.map { it.key }
        assertThat(attributeKeys).containsExactly("service.name", "service.version", "os.name")
    }

    @Test
    fun `GIVEN app goes background WHEN onTrimMemory THEN pending metrics are flushed`() = runTest {
        // Arrange
        val client = createClient()
        client.getMeter().counterBuilder("app_test_background").build().add(1)

        // Act
        (componentCallbacks.captured as ComponentCallbacks2)
            .onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN)

        // Assert
        val exported = networkExporter.exportedBatches.flatten()
        assertThat(exported.map { it.name }).contains("app_test_background")
    }

    private class InMemoryMetricStorage : SignalStorage.Metric {

        private val batches = ArrayDeque<Collection<MetricData>>()

        override fun write(items: Collection<MetricData>): CompletableResultCode {
            batches.addLast(items.toList())
            return CompletableResultCode.ofSuccess()
        }

        override fun clear(): CompletableResultCode {
            batches.clear()
            return CompletableResultCode.ofSuccess()
        }

        override fun close() = Unit

        override fun iterator(): MutableIterator<Collection<MetricData>> {
            return object : MutableIterator<Collection<MetricData>> {
                override fun hasNext(): Boolean = batches.isNotEmpty()
                override fun next(): Collection<MetricData> = batches.removeFirst()
                override fun remove() = Unit
            }
        }
    }

    private class RecordingMetricExporter : MetricExporter {

        val exportedBatches = mutableListOf<List<MetricData>>()
        val successfulBatches = mutableListOf<List<MetricData>>()
        var shouldFail = false

        override fun export(metrics: Collection<MetricData>): CompletableResultCode {
            exportedBatches.add(metrics.toList())
            return if (shouldFail) {
                CompletableResultCode.ofFailure()
            } else {
                successfulBatches.add(metrics.toList())
                CompletableResultCode.ofSuccess()
            }
        }

        override fun flush(): CompletableResultCode = CompletableResultCode.ofSuccess()

        override fun shutdown(): CompletableResultCode = CompletableResultCode.ofSuccess()

        override fun getAggregationTemporality(instrumentType: InstrumentType): AggregationTemporality {
            return AggregationTemporality.DELTA
        }
    }
}