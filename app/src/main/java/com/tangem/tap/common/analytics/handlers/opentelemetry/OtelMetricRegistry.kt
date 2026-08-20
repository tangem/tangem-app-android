package com.tangem.tap.common.analytics.handlers.opentelemetry

/**
 * Allowlist of analytics events mirrored to OTLP as metrics. Only listed events are exported, and
 * only their [OtelMetricSpec.allowedParams] become attributes — free-form params (addresses, raw
 * error strings) must stay out to keep the metric cardinality bounded.
 */
internal class OtelMetricRegistry(private val specs: Map<String, OtelMetricSpec>) {

    fun specFor(eventId: String): OtelMetricSpec? = specs[eventId]

    companion object {

        /** Pilot metrics are registered by the Store and GROW mirroring tasks. */
        val PILOT = OtelMetricRegistry(specs = emptyMap())

        fun counter(category: String, event: String, allowedParams: Set<String> = emptySet()): OtelMetricSpec {
            return OtelMetricSpec(
                metricName = OtelNameConverter.metricName(category, event),
                instrument = OtelInstrument.COUNTER,
                allowedParams = allowedParams,
            )
        }

        fun histogram(
            category: String,
            event: String,
            valueParam: String,
            allowedParams: Set<String> = emptySet(),
        ): OtelMetricSpec {
            return OtelMetricSpec(
                metricName = OtelNameConverter.metricName(category, event),
                instrument = OtelInstrument.HISTOGRAM,
                allowedParams = allowedParams,
                valueParam = valueParam,
            )
        }
    }
}

internal data class OtelMetricSpec(
    val metricName: String,
    val instrument: OtelInstrument,
    val allowedParams: Set<String>,
    val valueParam: String? = null,
)

internal enum class OtelInstrument {
    COUNTER,
    HISTOGRAM,
}