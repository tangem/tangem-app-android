package com.tangem.features.onboarding.v2.visa.impl.child.welcome.model.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.error.UniversalError

internal const val ONBOARDING_SOURCE = "Onboarding"
internal const val MAIN_SOURCE = "Main"

internal sealed class VisaAnalyticsEvent(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent("Visa", event, params) {

    data class ErrorOnboarding(val error: UniversalError) : VisaAnalyticsEvent(
        event = "Errors",
        params = mapOf(
            "Error Code" to error.errorCode.toString(),
            "Source" to ONBOARDING_SOURCE,
        ),
    )
}