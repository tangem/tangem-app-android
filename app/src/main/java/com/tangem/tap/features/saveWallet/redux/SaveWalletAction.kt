package com.tangem.tap.features.saveWallet.redux

import com.tangem.common.core.TangemError
import com.tangem.domain.common.ScanResponse
import org.rekotlin.Action

internal sealed interface SaveWalletAction : Action {
    data class ProvideBackupInfo(
        val scanResponse: ScanResponse,
        val accessCode: String?,
        val backupCardsIds: Set<String>?,
    ) : SaveWalletAction

    object Save : SaveWalletAction {
        object Success : SaveWalletAction
        data class Error(val error: TangemError) : SaveWalletAction
    }

    object Dismiss : SaveWalletAction

    object CloseError : SaveWalletAction
    object EnrollBiometrics : SaveWalletAction {
        object Enroll : SaveWalletAction
        object Cancel : SaveWalletAction
    }

    object SaveWalletWasShown : SaveWalletAction
}
