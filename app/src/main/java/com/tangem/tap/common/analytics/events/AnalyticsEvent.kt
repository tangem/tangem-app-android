package com.tangem.tap.common.analytics.events

/**
[REDACTED_AUTHOR]
 */
sealed class AnalyticsEvent(
    val category: String,
    val event: String,
    val params: Map<String, String> = mapOf(),
)