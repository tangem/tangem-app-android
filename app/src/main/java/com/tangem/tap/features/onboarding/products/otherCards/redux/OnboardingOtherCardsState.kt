package com.tangem.tap.features.onboarding.products.otherCards.redux

import com.tangem.tap.features.wallet.redux.Artwork
import org.rekotlin.StateType

/**
[REDACTED_AUTHOR]
 */
data class OnboardingOtherCardsState(
    // UI
    val cardArtwork: Artwork? = null,
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