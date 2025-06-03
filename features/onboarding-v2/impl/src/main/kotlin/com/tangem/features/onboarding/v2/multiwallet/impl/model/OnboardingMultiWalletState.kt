package com.tangem.features.onboarding.v2.multiwallet.impl.model

import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.wallets.models.UserWallet

data class OnboardingMultiWalletState(
    val currentStep: Step,
    val accessCode: AccessCode?,
    val isThreeCards: Boolean,
    val currentScanResponse: ScanResponse,
    val startFromFinalize: FinalizeStage?,
    val resultUserWallet: UserWallet.Cold?,
) {
    enum class FinalizeStage {
        ScanPrimaryCard, ScanBackupFirstCard, ScanBackupSecondCard
    }

    /**
     * Wallet1
     *                     ScanPrimary -> |
     *                                    |
     * CreateWallet -> ChooseBackupOption -> AddBackupDevice -> Finalize -> [Done]
     *                                    |
     *                                    -> [Done]
     *
     * Wallet2/Ring
     * CreateWallet -> SeedPhrase -> AddBackupDevice -> Finalize -> [Done]
     *              |
     *              -> AddBackupDevice -> Finalize -> [Done]
     */
    enum class Step {
        CreateWallet, ChooseBackupOption, SeedPhrase, ScanPrimary, AddBackupDevice, Finalize, Done
    }

    @JvmInline
    value class AccessCode(val accessCode: String)
}