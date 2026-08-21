package com.tangem.tap.common.analytics.handlers.opentelemetry

import com.tangem.core.analytics.api.AnalyticsHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.OtelIncludedEvent
import com.tangem.core.analytics.models.OtelMetric
import com.tangem.tap.common.analytics.api.AnalyticsHandlerBuilder
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.metrics.DoubleHistogram
import io.opentelemetry.api.metrics.LongCounter
import java.util.concurrent.ConcurrentHashMap

/**
 * Mirrors [OtelIncludedEvent]-marked analytics events to OTLP metrics. Does nothing until the OTel
 * pipeline is initialized (feature toggle on + api key configured) and for unmarked events.
 */
internal class OpenTelemetryAnalyticsHandler(
    private val metricsHolder: OpenTelemetryMetricsHolder,
) : AnalyticsHandler {

    private val counters = ConcurrentHashMap<String, LongCounter>()
    private val histograms = ConcurrentHashMap<String, DoubleHistogram>()

    override fun id(): String = ID

    override fun send(event: AnalyticsEvent) {
        val meter = metricsHolder.getMeter() ?: return
        if (event !is OtelIncludedEvent) return
        val metric = event.otelMetric
        val metricName = OtelNameConverter.metricName(event.event)
        val attributes = buildAttributes(metric, event)

        when (metric) {
            is OtelMetric.Counter -> {
                counters
                    .getOrPut(metricName) { meter.counterBuilder(metricName).build() }
                    .add(1, attributes)
            }
            is OtelMetric.Histogram -> {
                val value = event.params[metric.valueParam]?.toDoubleOrNull() ?: return
                histograms
                    .getOrPut(metricName) { meter.histogramBuilder(metricName).build() }
                    .record(value, attributes)
            }
        }
    }

    private fun buildAttributes(metric: OtelMetric, event: AnalyticsEvent): Attributes {
        val builder = Attributes.builder()
        for (param in metric.allowedParams) {
            val value = event.params[param] ?: continue
            builder.put(OtelNameConverter.attributeKey(param), value)
        }
        builder.put(CATEGORY_ATTRIBUTE, OtelNameConverter.categoryValue(event.category))
        return builder.build()
    }

    companion object {
        const val ID = "OpenTelemetry"

        private const val CATEGORY_ATTRIBUTE = "category"
    }

    class Builder(
        private val metricsHolder: OpenTelemetryMetricsHolder,
    ) : AnalyticsHandlerBuilder {

        override fun build(data: AnalyticsHandlerBuilder.Data): AnalyticsHandler {
            return OpenTelemetryAnalyticsHandler(metricsHolder)
        }
    }
}