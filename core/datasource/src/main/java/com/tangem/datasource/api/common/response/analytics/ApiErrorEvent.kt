package com.tangem.datasource.api.common.response.analytics

import com.tangem.core.analytics.models.AnalyticsEvent

internal data class ApiErrorEvent(
    val endpoint: String,
    val code: Int,
    val message: String,
) : AnalyticsEvent(
    category = "Tangem API Service",
    event = "Exception",
    params = mapOf(
        "Endpoint" to endpoint,
        "Code" to code.toString(),
        "Message" to message,
    ),
)