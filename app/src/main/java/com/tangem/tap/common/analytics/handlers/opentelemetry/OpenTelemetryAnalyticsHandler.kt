package com.tangem.tap.common.analytics.handlers.opentelemetry

import com.tangem.core.analytics.api.AnalyticsHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.tap.common.analytics.api.AnalyticsHandlerBuilder
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.metrics.DoubleHistogram
import io.opentelemetry.api.metrics.LongCounter
import java.util.concurrent.ConcurrentHashMap

/**
 * Mirrors allowlisted analytics events to OTLP metrics. Does nothing until the OTel pipeline is
 * initialized (feature toggle on + api key configured) and for events missing from the registry.
 */
internal class OpenTelemetryAnalyticsHandler(
    private val metricsHolder: OpenTelemetryMetricsHolder,
    private val registry: OtelMetricRegistry = OtelMetricRegistry.PILOT,
) : AnalyticsHandler {

    private val counters = ConcurrentHashMap<String, LongCounter>()
    private val histograms = ConcurrentHashMap<String, DoubleHistogram>()

    override fun id(): String = ID

    override fun send(event: AnalyticsEvent) {
        val meter = metricsHolder.getMeter() ?: return
        val spec = registry.specFor(event.id) ?: return
        val attributes = buildAttributes(spec, event.params)

        when (spec.instrument) {
            OtelInstrument.COUNTER -> {
                counters
                    .getOrPut(spec.metricName) { meter.counterBuilder(spec.metricName).build() }
                    .add(1, attributes)
            }
            OtelInstrument.HISTOGRAM -> {
                val value = spec.valueParam?.let { event.params[it] }?.toDoubleOrNull() ?: return
                histograms
                    .getOrPut(spec.metricName) { meter.histogramBuilder(spec.metricName).build() }
                    .record(value, attributes)
            }
        }
    }

    private fun buildAttributes(spec: OtelMetricSpec, params: Map<String, String>): Attributes {
        val builder = Attributes.builder()
        for (param in spec.allowedParams) {
            val value = params[param] ?: continue
            builder.put(OtelNameConverter.attributeKey(param), value)
        }
        return builder.build()
    }

    companion object {
        const val ID = "OpenTelemetry"
    }

    class Builder(
        private val metricsHolder: OpenTelemetryMetricsHolder,
    ) : AnalyticsHandlerBuilder {

        override fun build(data: AnalyticsHandlerBuilder.Data): AnalyticsHandler {
            return OpenTelemetryAnalyticsHandler(metricsHolder)
        }
    }
}