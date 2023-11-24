package com.tangem.core.analytics.models

/**
[REDACTED_AUTHOR]
 */
open class AnalyticsEvent(
    val category: String,
    val event: String,
    var params: Map<String, String> = mapOf(),
    val error: Throwable? = null,
) {

    val id: String = "[$category] $event"
}

interface OneTimeAnalyticsEvent {

    val oneTimeEventId: String
}