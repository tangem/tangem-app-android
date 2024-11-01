package com.tangem.features.onboarding.v2.multiwallet.impl.model

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.onboarding.v2.impl.R

fun screenTitleByStep(step: OnboardingMultiWalletState.Step): TextReference = when (step) {
    OnboardingMultiWalletState.Step.GeneratePrivateKeys -> TODO()
    OnboardingMultiWalletState.Step.CreateWallet -> resourceReference(R.string.onboarding_create_wallet_header)
    OnboardingMultiWalletState.Step.AddBackupDevice -> TODO()
    OnboardingMultiWalletState.Step.FinishBackup -> TODO()
    OnboardingMultiWalletState.Step.Done -> TODO()
}