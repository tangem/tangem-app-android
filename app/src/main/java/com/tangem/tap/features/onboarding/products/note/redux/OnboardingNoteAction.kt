package com.tangem.tap.features.onboarding.products.note.redux

import com.tangem.tap.features.onboarding.service.OnboardingWalletBalance
import org.rekotlin.Action

/**
[REDACTED_AUTHOR]
 */
sealed class OnboardingNoteAction : Action {
    // from user, ui
    data class SetResources(val resources: AndroidResources) : OnboardingNoteAction()
    object CreateWallet : OnboardingNoteAction()
    object TopUp : OnboardingNoteAction()
    object ShowAddressInfoDialog : OnboardingNoteAction()

    // from redux
    object DetermineStepOfScreen : OnboardingNoteAction()
    object Done : OnboardingNoteAction()

    data class SetStepOfScreen(val step: OnboardingNoteStep) : OnboardingNoteAction()

    sealed class Balance {
        object Update : OnboardingNoteAction()
        data class Set(val balance: OnboardingWalletBalance) : OnboardingNoteAction()
    }

    sealed class Confetti {
        object Show : OnboardingNoteAction()
        object Hide : OnboardingNoteAction()
    }
}