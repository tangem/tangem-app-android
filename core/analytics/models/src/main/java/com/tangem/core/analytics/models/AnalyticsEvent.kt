package com.tangem.core.analytics.models

/**
[REDACTED_AUTHOR]
 */
@Suppress("UnnecessaryAbstractClass")
abstract class AnalyticsEvent(
    val category: String,
    val event: String,
    var params: Map<String, EventValue> = mapOf(),
    val error: Throwable? = null,
) {

    val id: String = "[$category] $event"

    protected companion object {
        fun Int.asStringValue() = EventValue.StringValue(this.toString())

        fun String.asStringValue() = EventValue.StringValue(this)

        fun List<String>.asListValue() = EventValue.ListValue(this)
    }
}