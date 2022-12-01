package com.tangem.tap.features.saveWallet.redux

import com.tangem.common.core.TangemError
import com.tangem.domain.common.ScanResponse
import org.rekotlin.StateType

data class SaveWalletState(
    val backupInfo: WalletBackupInfo? = null,
    val isSaveInProgress: Boolean = false,
    val needEnrollBiometrics: Boolean = false,
    val error: TangemError? = null,
) : StateType {
    data class WalletBackupInfo(
        val scanResponse: ScanResponse,
        val accessCode: String?,
        val backupCardsIds: Set<String>?,
    )
}
