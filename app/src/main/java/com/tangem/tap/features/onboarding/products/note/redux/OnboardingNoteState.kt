package com.tangem.tap.features.onboarding.products.note.redux

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.WalletManager
import com.tangem.tap.domain.TapError
import com.tangem.tap.features.onboarding.OnboardingWalletBalance
import com.tangem.tap.network.moonpay.MoonpayStatus
import com.tangem.tap.store
import org.rekotlin.StateType
import kotlin.properties.ReadOnlyProperty

/**
[REDACTED_AUTHOR]
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
) : StateType {

    val progress: Int
        get() = steps.indexOf(currentStep)

    val isBuyAllowed: Boolean by ReadOnlyProperty<Any, Boolean> { thisRef, property ->
        store.state.globalState.moonpayStatus?.canBuy(walletBalance) ?: false
    }
}

fun MoonpayStatus.canBuy(walletBalance: OnboardingWalletBalance): Boolean {
    if (walletBalance.currency.blockchain == Blockchain.Unknown) return false

    return isBuyAllowed && availableToBuy.contains(walletBalance.currency.currencySymbol)
}

enum class OnboardingNoteStep {
    None, CreateWallet, TopUpWallet, Done
}