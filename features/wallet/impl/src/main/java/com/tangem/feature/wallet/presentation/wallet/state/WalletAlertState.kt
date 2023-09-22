package com.tangem.feature.wallet.presentation.wallet.state

import androidx.compose.runtime.Immutable

@Immutable
internal sealed class WalletAlertState {

    data class WalletAlreadySignedHashes(val onUnderstandClick: () -> Unit) : WalletAlertState()
}