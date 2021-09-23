package com.tangem.tap.features.onboarding.redux

import org.rekotlin.StateType

data class OnboardingState(
    val onboardingData: OnboardingData? = null,
) : StateType

data class OnboardingOtherCardsState(
    val any: String? = null
) : StateType