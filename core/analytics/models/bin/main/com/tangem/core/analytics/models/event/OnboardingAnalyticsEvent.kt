package com.tangem.core.analytics.models.event

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam

sealed class OnboardingAnalyticsEvent(
    category: String,
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(category, event, params) {

    sealed class Onboarding(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : OnboardingAnalyticsEvent(category = "Error", event = event, params = params) {

        data class OfflineAttestationFailed(
            val source: AnalyticsParam.ScreensSources,
        ) : Onboarding(
            event = "Offline Attestation Failed",
            params = mapOf(AnalyticsParam.SOURCE to source.value),
        )
    }
}