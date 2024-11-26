package com.tangem.features.onboarding.v2.multiwallet.impl.model

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.onboarding.v2.impl.R

fun screenTitleByStep(step: OnboardingMultiWalletState.Step): TextReference = when (step) {
    OnboardingMultiWalletState.Step.CreateWallet ->
        resourceReference(R.string.onboarding_create_wallet_header)
    OnboardingMultiWalletState.Step.AddBackupDevice ->
        resourceReference(R.string.onboarding_navbar_title_creating_backup)
    OnboardingMultiWalletState.Step.Finalize ->
        resourceReference(R.string.onboarding_button_finalize_backup)
    else -> error("No title for the step")
}