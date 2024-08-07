package com.tangem.tap.features.onboarding.products.note.redux

import com.tangem.blockchain.common.WalletManager
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.tap.domain.TapError
import com.tangem.tap.features.onboarding.OnboardingWalletBalance
import org.rekotlin.StateType

/**
* [REDACTED_AUTHOR]
 */
data class OnboardingNoteState(
    val walletManager: WalletManager? = null,
    // UI
    val cardArtworkUrl: String? = null,
    val walletBalance: OnboardingWalletBalance = OnboardingWalletBalance.loading(),
    val balanceNonCriticalError: TapError? = null,
    val balanceCriticalError: TapError? = null,
    val currentStep: OnboardingNoteStep = OnboardingNoteStep.None,
    val steps: List<OnboardingNoteStep> = OnboardingNoteStep.values().toList(),
    val showConfetti: Boolean = false,
    val scanResponse: ScanResponse? = null,
) : StateType {

    val progress: Int
        get() = steps.indexOf(currentStep)
}

enum class OnboardingNoteStep {
    None, CreateWallet, TopUpWallet, Done
}
