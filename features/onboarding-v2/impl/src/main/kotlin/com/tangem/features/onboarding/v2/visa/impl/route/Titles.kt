package com.tangem.features.onboarding.v2.visa.impl.route

import com.tangem.core.ui.extensions.TextReference

// TODO add translations [REDACTED_TASK_KEY]
internal fun OnboardingVisaRoute.screenTitle(): TextReference = when (this) {
    OnboardingVisaRoute.AccessCode -> TextReference.Str("Access code")
    is OnboardingVisaRoute.ChooseWallet -> TextReference.Str("Account activation")
    OnboardingVisaRoute.InProgress -> TextReference.Str("In progress")
    is OnboardingVisaRoute.OtherWalletApproveOption -> TextReference.Str("Wallet connection")
    OnboardingVisaRoute.PinCode -> TextReference.Str("PIN code")
    is OnboardingVisaRoute.TangemWalletApproveOption -> TextReference.Str("Wallet connection")
    is OnboardingVisaRoute.Welcome -> TextReference.Str("Getting started")
}