package com.tangem.tap.features.onboarding.products.note.redux

import com.tangem.blockchain.common.WalletManager
import com.tangem.tap.domain.TapError
import com.tangem.tap.domain.extensions.buyIsAllowed
import com.tangem.tap.features.onboarding.OnboardingWalletBalance
import com.tangem.tap.store
import kotlin.properties.ReadOnlyProperty
import org.rekotlin.StateType

/**
 * Created by Anton Zhilenkov on 23/09/2021.
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
        store.state.globalState.currencyExchangeManager?.buyIsAllowed(walletBalance.currency) ?: false
    }
}

enum class OnboardingNoteStep {
    None, CreateWallet, TopUpWallet, Done
}
