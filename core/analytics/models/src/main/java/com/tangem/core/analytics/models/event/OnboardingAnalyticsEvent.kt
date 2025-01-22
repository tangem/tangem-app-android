package com.tangem.core.analytics.models.event

import com.tangem.core.analytics.models.AnalyticsEvent

sealed class OnboardingAnalyticsEvent(
    category: String,
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(category, event, params) {

    sealed class Onboarding(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : OnboardingAnalyticsEvent(category = "Onboarding", event = event, params = params) {

        data object OfflineAttestationFailed : Onboarding(event = "Offline Attestation Failed")
    }
}