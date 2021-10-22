package com.tangem.tap.features.onboarding.products.note.redux

import com.tangem.blockchain.common.WalletManager
import com.tangem.tap.features.onboarding.OnboardingWalletBalance
import com.tangem.tap.features.wallet.redux.Artwork
import org.rekotlin.StateType

/**
[REDACTED_AUTHOR]
 */
data class OnboardingNoteState(
    val walletManager: WalletManager? = null,
    val resources: OnboardingNoteResources = OnboardingNoteResources(),
    // UI
    val cardArtwork: Artwork? = null,
    val walletBalance: OnboardingWalletBalance = OnboardingWalletBalance.loading(),
    val currentStep: OnboardingNoteStep = OnboardingNoteStep.None,
    val steps: List<OnboardingNoteStep> = OnboardingNoteStep.values().toList(),
    val showConfetti: Boolean = false,
) : StateType {

    val progress: Int
        get() = steps.indexOf(currentStep)
}

enum class OnboardingNoteStep {
    None, CreateWallet, TopUpWallet, Done
}

data class OnboardingNoteResources(
    val strings: Strings = Strings()
) {
    data class Strings(
        val addressWasCopied: Int = -1
    )
}