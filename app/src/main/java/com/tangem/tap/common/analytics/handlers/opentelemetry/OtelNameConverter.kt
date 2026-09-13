package com.tangem.tap.common.analytics.handlers.opentelemetry

/**
 * Converts analytics event coordinates to OTel-valid plain snake_case names — the infra requirement,
 * free-form Amplitude ids (`[Basic] Transaction sent`) cannot be used as is. The metric name is the
 * event name alone (`transaction_sent`); the category is exported as the `category` attribute. The
 * scheme must stay in sync with the iOS app so both platforms report the same metric names.
 */
internal object OtelNameConverter {

    private val invalidChars = Regex(pattern = "[^a-z0-9]+")

    fun metricName(event: String): String = segment(event)

    fun attributeKey(param: String): String = segment(param)

    fun categoryValue(category: String): String = segment(category)

    private fun segment(raw: String): String {
        return raw.lowercase().replace(invalidChars, "_").trim('_')
    }
}