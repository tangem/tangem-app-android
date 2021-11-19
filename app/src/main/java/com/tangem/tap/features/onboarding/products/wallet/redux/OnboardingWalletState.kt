package com.tangem.tap.features.onboarding.products.wallet.redux

import org.rekotlin.StateType

/**
[REDACTED_AUTHOR]
 */
data class OnboardingWalletState(
    val any: String? = null
) : StateType

enum class OnboardingWalletStep {
    None, CreateWallet, TopUpWallet, Backup, Done
}