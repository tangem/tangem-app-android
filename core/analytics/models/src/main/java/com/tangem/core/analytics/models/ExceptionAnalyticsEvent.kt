package com.tangem.core.analytics.models

class ExceptionAnalyticsEvent(
    val exception: Throwable,
    // val category: String,
    // val event: String,
    var params: Map<String, String> = mapOf(),
)