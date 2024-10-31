package com.tangem.features.onboarding.v2.multiwallet.impl.model

import com.tangem.domain.models.scan.ScanResponse

data class OnboardingMultiWalletState(
    val currentStep: Step,
    val currentScanResponse: ScanResponse,
) {

    enum class Step {
        CreateWallet, AddBackupDevice, FinishBackup, Done
    }
}
