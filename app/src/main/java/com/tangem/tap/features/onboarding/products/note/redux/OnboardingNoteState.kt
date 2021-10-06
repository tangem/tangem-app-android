package com.tangem.tap.features.onboarding.products.note.redux

import android.graphics.Bitmap
import com.tangem.blockchain.common.Blockchain
import com.tangem.tap.features.onboarding.service.OnboardingNoteService
import com.tangem.tap.features.wallet.redux.Currency
import com.tangem.tap.features.wallet.redux.ProgressState
import org.rekotlin.StateType
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
data class OnboardingNoteState(
    val onboardingService: OnboardingNoteService? = null,
    val resources: AndroidResources = AndroidResources(),
        // UI
    val showConfetti: Boolean = false,
    val artworkBitmap: Bitmap? = null,
    val balanceValue: BigDecimal = BigDecimal.ZERO,
    val balanceCurrency: Currency = Currency.Blockchain(Blockchain.Unknown),
    val balanceState: ProgressState = ProgressState.Done,
    val amountToCreateAccount: String? = null,
    val currentStep: OnboardingNoteStep = OnboardingNoteStep.None,
    val steps: List<OnboardingNoteStep> = OnboardingNoteStep.values().toList(),
) : StateType {

    val progress: Int
        get() = steps.indexOf(currentStep)
}

enum class OnboardingNoteStep {
    None, CreateWallet, TopUpWallet, Done
}

data class AndroidResources(
    val strings: OnboardingStringResources = OnboardingStringResources()
)

data class OnboardingStringResources(
    val addressWasCopied: Int = -1
)