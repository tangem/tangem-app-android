package com.tangem.features.onboarding.v2.note.impl.route

internal fun OnboardingNoteRoute.stepNum() = when (this) {
    OnboardingNoteRoute.CreateWallet -> 1
    OnboardingNoteRoute.TopUp -> 2
}