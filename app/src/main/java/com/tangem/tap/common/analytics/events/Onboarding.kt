package com.tangem.tap.common.analytics.events

import com.tangem.core.analytics.models.AnalyticsEvent

/**
[REDACTED_AUTHOR]
 */
sealed class Onboarding(
    category: String,
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category, event, params) {

    class Finished : Onboarding("Onboarding", "Onboarding Finished")
}