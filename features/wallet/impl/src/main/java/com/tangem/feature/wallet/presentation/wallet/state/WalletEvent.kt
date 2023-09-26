package com.tangem.feature.wallet.presentation.wallet.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

@Immutable
internal sealed class WalletEvent {

    data class ChangeWallet(val index: Int) : WalletEvent()

    data class ShowError(val text: TextReference) : WalletEvent()

    data class ShowToast(val text: TextReference) : WalletEvent()

    data class ShowAlert(
        val title: TextReference,
        val message: TextReference,
        val onActionClick: (() -> Unit)?,
    ) : WalletEvent()

    data class CopyAddress(val address: String) : WalletEvent()

    data class ShowWalletAlreadySignedHashesMessage(val onUnderstandClick: () -> Unit) : WalletEvent()

    data class RateApp(val onDismissClick: () -> Unit) : WalletEvent()
}