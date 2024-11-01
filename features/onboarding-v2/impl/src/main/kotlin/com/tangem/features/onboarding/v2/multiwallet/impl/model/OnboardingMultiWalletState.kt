package com.tangem.features.onboarding.v2.multiwallet.impl.model

data class OnboardingMultiWalletState(
    val currentStep: Step,
) {

    enum class Step {
        GeneratePrivateKeys, CreateWallet, AddBackupDevice, FinishBackup, Done
    }
}