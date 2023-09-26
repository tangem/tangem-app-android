package com.tangem.feature.wallet.presentation.wallet.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

@Immutable
internal sealed class WalletAlertState {

    data class WalletAlreadySignedHashes(val onUnderstandClick: () -> Unit) : WalletAlertState()

    data class DefaultAlert(
        val title: TextReference,
        val message: TextReference,
        val onActionClick: (() -> Unit)?,
    ) : WalletAlertState()
}