package com.tangem.tap.common.analytics.handlers.opentelemetry

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.OtelIncludedEvent
import com.tangem.core.analytics.models.OtelMetric
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.metrics.DoubleHistogram
import io.opentelemetry.api.metrics.DoubleHistogramBuilder
import io.opentelemetry.api.metrics.LongCounter
import io.opentelemetry.api.metrics.LongCounterBuilder
import io.opentelemetry.api.metrics.Meter
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class OpenTelemetryAnalyticsHandlerTest {

    private val counter: LongCounter = mockk(relaxed = true)
    private val counterBuilder: LongCounterBuilder = mockk()
    private val histogram: DoubleHistogram = mockk(relaxed = true)
    private val histogramBuilder: DoubleHistogramBuilder = mockk()
    private val meter: Meter = mockk()
    private val metricsHolder: OpenTelemetryMetricsHolder = mockk()

    private val handler = OpenTelemetryAnalyticsHandler(metricsHolder = metricsHolder)

    @BeforeEach
    fun setupMocks() {
        clearMocks(counter, counterBuilder, histogram, histogramBuilder, meter, metricsHolder)
        every { metricsHolder.getMeter() } returns meter
        every { meter.counterBuilder(any()) } returns counterBuilder
        every { counterBuilder.build() } returns counter
        every { meter.histogramBuilder(any()) } returns histogramBuilder
        every { histogramBuilder.build() } returns histogram
    }

    private class TransactionSentEvent(
        params: Map<String, String> = emptyMap(),
    ) : AnalyticsEvent(category = "Basic", event = "Transaction sent", params = params), OtelIncludedEvent {

        override val otelMetric = OtelMetric.Counter(allowedParams = setOf("Blockchain", "Fee type"))
    }

    private class ExpressDurationEvent(
        params: Map<String, String> = emptyMap(),
    ) : AnalyticsEvent(category = "Swap", event = "Express Duration", params = params), OtelIncludedEvent {

        override val otelMetric = OtelMetric.Histogram(valueParam = "Duration", allowedParams = setOf("Provider"))
    }

    @Test
    fun `GIVEN pipeline not initialized WHEN send THEN nothing is recorded`() {
        // Arrange
        every { metricsHolder.getMeter() } returns null

        // Act
        handler.send(TransactionSentEvent())

        // Assert
        verify(exactly = 0) { meter.counterBuilder(any()) }
    }

    @Test
    fun `GIVEN unmarked event WHEN send THEN nothing is recorded`() {
        // Act
        handler.send(AnalyticsEvent(category = "Basic", event = "Card Was Scanned"))

        // Assert
        verify(exactly = 0) { meter.counterBuilder(any()) }
        verify(exactly = 0) { meter.histogramBuilder(any()) }
    }

    @Test
    fun `GIVEN counter event WHEN send THEN counter incremented with allowlisted normalized attributes`() {
        // Arrange
        val event = TransactionSentEvent(
            params = mapOf(
                "Blockchain" to "Bitcoin",
                "Fee type" to "Fixed",
                "Token" to "BTC",
            ),
        )
        val expectedAttributes = Attributes.of(
            AttributeKey.stringKey("blockchain"), "Bitcoin",
            AttributeKey.stringKey("fee_type"), "Fixed",
            AttributeKey.stringKey("category"), "basic",
        )

        // Act
        handler.send(event)

        // Assert
        verify(exactly = 1) { meter.counterBuilder("transaction_sent") }
        verify(exactly = 1) { counter.add(1, expectedAttributes) }
    }

    @Test
    fun `GIVEN two sends of one metric WHEN send THEN instrument is built once`() {
        // Act
        handler.send(TransactionSentEvent())
        handler.send(TransactionSentEvent())

        // Assert
        verify(exactly = 1) { meter.counterBuilder("transaction_sent") }
        verify(exactly = 2) { counter.add(1, Attributes.of(AttributeKey.stringKey("category"), "basic")) }
    }

    @Test
    fun `GIVEN histogram event WHEN send THEN value recorded from the value param`() {
        // Arrange
        val event = ExpressDurationEvent(params = mapOf("Duration" to "12.5", "Provider" to "ChangeNow"))
        val expectedAttributes = Attributes.of(
            AttributeKey.stringKey("provider"), "ChangeNow",
            AttributeKey.stringKey("category"), "swap",
        )

        // Act
        handler.send(event)

        // Assert
        verify(exactly = 1) { histogram.record(12.5, expectedAttributes) }
    }

    @Test
    fun `GIVEN histogram value missing or non-numeric WHEN send THEN nothing is recorded`() {
        // Act
        handler.send(ExpressDurationEvent())
        handler.send(ExpressDurationEvent(params = mapOf("Duration" to "fast")))

        // Assert
        verify(exactly = 0) { histogram.record(any(), any()) }
    }

    @Test
    fun `GIVEN handler WHEN id THEN OpenTelemetry`() {
        // Act & Assert
        assertThat(handler.id()).isEqualTo("OpenTelemetry")
    }
}