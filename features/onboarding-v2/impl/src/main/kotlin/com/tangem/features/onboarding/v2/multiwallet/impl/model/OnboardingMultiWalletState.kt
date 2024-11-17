package com.tangem.features.onboarding.v2.multiwallet.impl.model

import com.tangem.domain.models.scan.ScanResponse

data class OnboardingMultiWalletState(
    val currentStep: Step,
    val currentScanResponse: ScanResponse,
) {
    /**
     * Wallet1
     * CreateWallet -> ChooseBackupOption -> AddBackupDevice -> FinishBackup -> [Done]
     *                                    |
     *                                    -> [Done]
     *
     * Wallet2/Ring
     * CreateWallet -> SeedPhrase -> AddBackupDevice -> FinishBackup -> [Done]
     *              |
     *              -> AddBackupDevice -> FinishBackup -> [Done]
     */
    enum class Step {
        CreateWallet, ChooseBackupOption, SeedPhrase, AddBackupDevice, FinishBackup, Done
    }
}
