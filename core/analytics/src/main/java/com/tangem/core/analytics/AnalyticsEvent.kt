package com.tangem.core.analytics

/**
* [REDACTED_AUTHOR]
 */
open class AnalyticsEvent(
    val category: String,
    val event: String,
    var params: Map<String, String> = mapOf(),
    val error: Throwable? = null,
    var filterData: Any? = null,
)
