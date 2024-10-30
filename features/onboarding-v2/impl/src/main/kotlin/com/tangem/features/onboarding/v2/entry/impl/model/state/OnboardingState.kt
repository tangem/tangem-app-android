package com.tangem.features.onboarding.v2.entry.impl.model.state

import com.tangem.features.onboarding.v2.entry.impl.routing.OnboardingRoute

internal data class OnboardingState(
    val currentRoute: OnboardingRoute,
)