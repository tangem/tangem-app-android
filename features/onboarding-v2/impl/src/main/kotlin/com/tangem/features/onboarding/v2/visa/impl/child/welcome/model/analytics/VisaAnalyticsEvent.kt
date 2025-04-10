package com.tangem.features.onboarding.v2.visa.impl.child.welcome.model.analytics

import com.tangem.core.analytics.models.AnalyticsEvent

internal const val ONBOARDING_SOURCE = "Onboarding"
internal const val MAIN_SOURCE = "Main"

internal sealed class VisaAnalyticsEvent(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent("Visa", event, params) {

    data class Errors(
        val errorCode: String,
        val source: String,
    ) : VisaAnalyticsEvent(
        event = "Errors",
        params = mapOf(
            "Error Code" to errorCode,
            "Source" to source,
        ),
    )
}