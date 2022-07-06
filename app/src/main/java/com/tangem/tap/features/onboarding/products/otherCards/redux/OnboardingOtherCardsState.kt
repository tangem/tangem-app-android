package com.tangem.tap.features.onboarding.products.otherCards.redux

import org.rekotlin.StateType

/**
 * Created by Anton Zhilenkov on 23/09/2021.
 */
data class OnboardingOtherCardsState(
    // UI
    val cardArtworkUrl: String? = null,
    val showConfetti: Boolean = false,
    val currentStep: OnboardingOtherCardsStep = OnboardingOtherCardsStep.None,
    val steps: List<OnboardingOtherCardsStep> = OnboardingOtherCardsStep.values().toList(),
) : StateType {

    val progress: Int
        get() = steps.indexOf(currentStep)
}

enum class OnboardingOtherCardsStep {
    None, CreateWallet, Done
}
