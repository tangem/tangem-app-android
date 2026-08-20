package com.tangem.tap.common.analytics.handlers.opentelemetry

/**
 * Converts analytics event coordinates to OTel-valid snake_case names. Instrument names must match
 * `[A-Za-z][A-Za-z0-9_.\-/]*`, so the free-form Amplitude ids (`[Basic] Transaction sent`) cannot be
 * used as is. The scheme is `app.<category>.<event>` and must stay in sync with the iOS app so both
 * platforms report the same metric names.
 */
internal object OtelNameConverter {

    private const val METRIC_PREFIX = "app"

    private val invalidChars = Regex(pattern = "[^a-z0-9]+")

    fun metricName(category: String, event: String): String {
        return "$METRIC_PREFIX.${segment(category)}.${segment(event)}"
    }

    fun attributeKey(param: String): String = segment(param)

    private fun segment(raw: String): String {
        return raw.lowercase().replace(invalidChars, "_").trim('_')
    }
}