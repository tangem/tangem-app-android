package com.tangem.features.onboarding.v2

interface OnboardingV2FeatureToggles {
    val isOnboardingV2Enabled: Boolean
    val isVisaOnboardingEnabled: Boolean
    val isNoteRefactoringEnabled: Boolean
    val isTwinRefactoringEnabled: Boolean
}