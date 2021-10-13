package com.tangem.tap.features.onboarding.products.otherCards.redux

import android.graphics.Bitmap
import com.tangem.tap.features.onboarding.service.OnboardingOtherCardsService
import org.rekotlin.StateType

/**
[REDACTED_AUTHOR]
 */
data class OnboardingOtherCardsState(
    val onboardingService: OnboardingOtherCardsService? = null,
        // UI
    val showConfetti: Boolean = false,
    val artworkBitmap: Bitmap? = null,
    val currentStep: OnboardingOtherCardsStep = OnboardingOtherCardsStep.None,
    val steps: List<OnboardingOtherCardsStep> = OnboardingOtherCardsStep.values().toList(),
) : StateType {

    val progress: Int
        get() = steps.indexOf(currentStep)
}

enum class OnboardingOtherCardsStep {
    None, CreateWallet, Done
}