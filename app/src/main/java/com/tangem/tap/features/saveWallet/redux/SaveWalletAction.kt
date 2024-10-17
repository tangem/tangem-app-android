package com.tangem.tap.features.saveWallet.redux

import com.tangem.common.core.TangemError
import com.tangem.domain.models.scan.ScanResponse
import org.rekotlin.Action

internal sealed interface SaveWalletAction : Action {
    data class ProvideBackupInfo(
        val scanResponse: ScanResponse,
        val accessCode: String?,
        val backupCardsIds: Set<String>?,
    ) : SaveWalletAction

    data object AllowToUseBiometrics : SaveWalletAction {
        data object Success : SaveWalletAction
        data class Error(val error: TangemError) : SaveWalletAction
    }

    data object Dismiss : SaveWalletAction

    data object CloseError : SaveWalletAction
    data object EnrollBiometrics : SaveWalletAction {
        data object Enroll : SaveWalletAction
        data object Cancel : SaveWalletAction
    }

    data class SaveWalletAfterBackup(
        val hasBackupError: Boolean,
        val shouldNavigateToWallet: Boolean,
    ) : SaveWalletAction
}