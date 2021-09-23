package com.tangem.tap.features.onboarding.redux

import org.rekotlin.Action

/**
[REDACTED_AUTHOR]
 */
sealed class OnboardingAction : Action {

    data class SetInitialStepOfScreen(val step: OnboardingStep) : OnboardingAction()
    object SwitchToNextStep : OnboardingAction()

    data class SetData(val data: OnboardingData) : OnboardingAction()

    sealed class OnboardingOtherAction : Action {
        object Init : OnboardingOtherAction()
        object CreateWallet : OnboardingOtherAction()
        object Done : OnboardingOtherAction()
    }

    class Error(val message: String) : OnboardingAction()
}