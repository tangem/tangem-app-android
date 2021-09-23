package com.tangem.tap.features.onboarding.products.note.redux

import com.tangem.blockchain.common.WalletManager
import com.tangem.tap.features.onboarding.redux.OnboardingNoteStep
import org.rekotlin.StateType

/**
[REDACTED_AUTHOR]
 */
data class OnboardingNoteState(
    val walletManager: WalletManager? = null,
    val currentStep: OnboardingNoteStep = OnboardingNoteStep.None,
    val steps: List<OnboardingNoteStep> = OnboardingNoteStep.values().toList(),
) : StateType {

    val progress: Int
        get() = steps.indexOf(currentStep)
}