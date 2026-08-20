package com.tangem.tap.common.analytics.handlers.opentelemetry

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.models.AnalyticsEvent
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

    private val registry = OtelMetricRegistry(
        specs = mapOf(
            "[Basic] Transaction sent" to OtelMetricRegistry.counter(
                category = "Basic",
                event = "Transaction sent",
                allowedParams = setOf("Blockchain", "Fee type"),
            ),
            "[Swap] Express Duration" to OtelMetricRegistry.histogram(
                category = "Swap",
                event = "Express Duration",
                valueParam = "Duration",
                allowedParams = setOf("Provider"),
            ),
        ),
    )

    private val handler = OpenTelemetryAnalyticsHandler(metricsHolder = metricsHolder, registry = registry)

    @BeforeEach
    fun setupMocks() {
        clearMocks(counter, counterBuilder, histogram, histogramBuilder, meter, metricsHolder)
        every { metricsHolder.getMeter() } returns meter
        every { meter.counterBuilder(any()) } returns counterBuilder
        every { counterBuilder.build() } returns counter
        every { meter.histogramBuilder(any()) } returns histogramBuilder
        every { histogramBuilder.build() } returns histogram
    }

    @Test
    fun `GIVEN pipeline not initialized WHEN send THEN nothing is recorded`() {
        // Arrange
        every { metricsHolder.getMeter() } returns null

        // Act
        handler.send(AnalyticsEvent(category = "Basic", event = "Transaction sent"))

        // Assert
        verify(exactly = 0) { meter.counterBuilder(any()) }
    }

    @Test
    fun `GIVEN event not in registry WHEN send THEN nothing is recorded`() {
        // Act
        handler.send(AnalyticsEvent(category = "Basic", event = "Card Was Scanned"))

        // Assert
        verify(exactly = 0) { meter.counterBuilder(any()) }
        verify(exactly = 0) { meter.histogramBuilder(any()) }
    }

    @Test
    fun `GIVEN counter spec WHEN send THEN counter incremented with allowlisted normalized attributes`() {
        // Arrange
        val event = AnalyticsEvent(
            category = "Basic",
            event = "Transaction sent",
            params = mapOf(
                "Blockchain" to "Bitcoin",
                "Fee type" to "Fixed",
                "Token" to "BTC",
            ),
        )
        val expectedAttributes = Attributes.of(
            AttributeKey.stringKey("blockchain"), "Bitcoin",
            AttributeKey.stringKey("fee_type"), "Fixed",
        )

        // Act
        handler.send(event)

        // Assert
        verify(exactly = 1) { meter.counterBuilder("app_basic_transaction_sent") }
        verify(exactly = 1) { counter.add(1, expectedAttributes) }
    }

    @Test
    fun `GIVEN two sends of one metric WHEN send THEN instrument is built once`() {
        // Arrange
        val event = AnalyticsEvent(category = "Basic", event = "Transaction sent")

        // Act
        handler.send(event)
        handler.send(event)

        // Assert
        verify(exactly = 1) { meter.counterBuilder("app_basic_transaction_sent") }
        verify(exactly = 2) { counter.add(1, Attributes.empty()) }
    }

    @Test
    fun `GIVEN histogram spec WHEN send THEN value recorded from the value param`() {
        // Arrange
        val event = AnalyticsEvent(
            category = "Swap",
            event = "Express Duration",
            params = mapOf("Duration" to "12.5", "Provider" to "ChangeNow"),
        )
        val expectedAttributes = Attributes.of(AttributeKey.stringKey("provider"), "ChangeNow")

        // Act
        handler.send(event)

        // Assert
        verify(exactly = 1) { histogram.record(12.5, expectedAttributes) }
    }

    @Test
    fun `GIVEN histogram value missing or non-numeric WHEN send THEN nothing is recorded`() {
        // Act
        handler.send(AnalyticsEvent(category = "Swap", event = "Express Duration"))
        handler.send(
            AnalyticsEvent(category = "Swap", event = "Express Duration", params = mapOf("Duration" to "fast")),
        )

        // Assert
        verify(exactly = 0) { histogram.record(any(), any()) }
    }

    @Test
    fun `GIVEN handler WHEN id THEN OpenTelemetry`() {
        // Act & Assert
        assertThat(handler.id()).isEqualTo("OpenTelemetry")
    }
}