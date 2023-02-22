package com.tangem.tap.features.onboarding.products.otherCards.redux

import org.rekotlin.Action

/**
[REDACTED_AUTHOR]
 */
sealed class OnboardingOtherCardsAction : Action {
    // from user, ui
    object Init : OnboardingOtherCardsAction()
    object LoadCardArtwork : OnboardingOtherCardsAction()
    object CreateWallet : OnboardingOtherCardsAction()

    // from redux
    class SetArtworkUrl(val artworkUrl: String) : OnboardingOtherCardsAction()
    object DetermineStepOfScreen : OnboardingOtherCardsAction()
    object Done : OnboardingOtherCardsAction()
    object OnBackPressed : OnboardingOtherCardsAction()

    data class SetStepOfScreen(val step: OnboardingOtherCardsStep) : OnboardingOtherCardsAction()

    sealed class Confetti {
        object Show : OnboardingOtherCardsAction()
        object Hide : OnboardingOtherCardsAction()
    }
}