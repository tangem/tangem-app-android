package com.tangem.tap.features.onboarding.products.otherCards.redux

import org.rekotlin.Action

/**
[REDACTED_AUTHOR]
 */
sealed class OnboardingOtherCardsAction : Action {
    // from user, ui
    object CreateWallet : OnboardingOtherCardsAction()

    // from redux
    object DetermineStepOfScreen : OnboardingOtherCardsAction()
    object Done : OnboardingOtherCardsAction()

    data class SetStepOfScreen(val step: OnboardingOtherCardsStep) : OnboardingOtherCardsAction()

    sealed class Confetti {
        object Show : OnboardingOtherCardsAction()
        object Hide : OnboardingOtherCardsAction()
    }
}