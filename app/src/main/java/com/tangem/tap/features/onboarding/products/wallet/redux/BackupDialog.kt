package com.tangem.tap.features.onboarding.products.wallet.redux

import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.redux.StateDialog

sealed class BackupDialog : StateDialog {
    data class UnfinishedBackupFound(
        val scanResponse: ScanResponse? = null,
    ) : BackupDialog()

    data class ConfirmDiscardingBackup(
        val scanResponse: ScanResponse? = null,
    ) : BackupDialog()
}