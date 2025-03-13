package com.tangem.features.onboarding.v2.entry.impl.analytics

import com.tangem.core.analytics.models.AnalyticsEvent

internal sealed class OnboardingEntryEvent(
    category: String,
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(category, event, params) {

    data class Biometric(
        val state: State,
    ) : OnboardingEntryEvent(
        category = "Onboarding / Biometric",
        event = "Biometry",
        params = mapOf("State" to state.name),
    ) {
        enum class State {
            On, Off
        }
    }
}