package com.tangem.core.analytics.models

/**
 * Marks an [AnalyticsEvent] that is mirrored to the OTLP gateway as a metric. The metric name is
 * the event's own name in snake_case (`Transaction sent` -> `transaction_sent`) and the category is
 * exported as the `category` attribute, so marking a class is enough — there is no separate name to
 * keep in sync.
 */
interface OtelIncludedEvent {

    val otelMetric: OtelMetric
}

/**
 * How the mirrored event is aggregated on the OTel side. [allowedParams] lists the event params
 * exported as metric attributes — everything else is dropped to keep the metric cardinality bounded,
 * so free-form values (addresses, raw error strings) must stay out.
 */
sealed interface OtelMetric {

    val allowedParams: Set<String>

    /**
     * Counts event occurrences: every event adds 1 to the metric. With delta temporality each
     * export carries the number of occurrences since the previous export. For fact-of-action

     */
    data class Counter(
        override val allowedParams: Set<String> = emptySet(),
    ) : OtelMetric

    /**
     * Records the distribution of a numeric value carried by the [valueParam] event param;
     * events with a missing or non-numeric value are skipped. For measurement events — e.g. an
     * express swap duration by provider.
     */
    data class Histogram(
        val valueParam: String,
        override val allowedParams: Set<String> = emptySet(),
    ) : OtelMetric
}