package com.tangem.core.analytics.models

class ExceptionAnalyticsEvent(
    val exception: Throwable,
    val params: Map<String, String> = mapOf(),
)