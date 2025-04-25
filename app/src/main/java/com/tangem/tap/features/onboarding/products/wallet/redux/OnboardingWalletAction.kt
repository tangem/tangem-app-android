package com.tangem.tap.features.onboarding.products.wallet.redux

import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.wallets.models.UserWalletId
import org.rekotlin.Action

sealed class OnboardingWalletAction : Action {
    data class WalletSaved(val userWalletId: UserWalletId) : OnboardingWalletAction()
}

sealed class BackupAction : Action {

    data object DiscardBackup : BackupAction()
    data object DiscardSavedBackup : BackupAction()

    data class ResumeFoundUnfinishedBackup(
        val unfinishedBackupScanResponse: ScanResponse?,
    ) : BackupAction()
}