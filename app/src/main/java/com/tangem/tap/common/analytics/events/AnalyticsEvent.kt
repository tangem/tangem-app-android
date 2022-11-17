package com.tangem.tap.common.analytics.events

/**
 * Created by Anton Zhilenkov on 28.09.2022.
 */
sealed class AnalyticsEvent(
    val category: String,
    val event: String,
    var params: Map<String, String> = mapOf(),
    val error: Throwable? = null,
    var filterData: Any? = null,
)
