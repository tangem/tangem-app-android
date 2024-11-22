package com.tangem.features.onboarding.v2.multiwallet.impl.model

import com.tangem.domain.models.scan.ScanResponse

data class OnboardingMultiWalletState(
    val currentStep: Step,
    val accessCode: AccessCode?,
    val currentScanResponse: ScanResponse,
) {
    /**
     * Wallet1
     * CreateWallet -> ChooseBackupOption -> AddBackupDevice -> Finalize -> [Done]
     *                                    |
     *                                    -> [Done]
     *
     * Wallet2/Ring
     * CreateWallet -> SeedPhrase -> AddBackupDevice -> FinishBackup -> [Done]
     *              |
     *              -> AddBackupDevice -> Finalize -> [Done]
     */
    enum class Step {
        CreateWallet, ChooseBackupOption, SeedPhrase, AddBackupDevice, Finalize, Done
    }

    @JvmInline
    value class AccessCode(val accessCode: String)
}
