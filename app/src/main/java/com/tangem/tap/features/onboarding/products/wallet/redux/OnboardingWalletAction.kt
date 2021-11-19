package com.tangem.tap.features.onboarding.products.wallet.redux

import org.rekotlin.Action

/**
[REDACTED_AUTHOR]
 */
sealed class OnboardingWalletAction : Action {
    object Init : OnboardingWalletAction()
    object CreateWallet : OnboardingWalletAction()
    object TopUp : OnboardingWalletAction()
    object Done : OnboardingWalletAction()

    object ProceedBackup : OnboardingWalletAction()

    sealed class Backup : Action {

    }
}