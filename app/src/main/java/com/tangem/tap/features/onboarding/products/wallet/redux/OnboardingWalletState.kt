package com.tangem.tap.features.onboarding.products.wallet.redux

import com.tangem.tap.features.onboarding.redux.OnboardingStep
import org.rekotlin.StateType

/**
[REDACTED_AUTHOR]
 */
data class OnboardingWalletState(
    val any: String? = null
) : StateType

enum class OnboardingWalletStep : OnboardingStep {
    None, CreateWallet, TopUpWallet, Backup, Done
}