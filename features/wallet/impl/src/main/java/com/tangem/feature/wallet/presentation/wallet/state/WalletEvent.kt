package com.tangem.feature.wallet.presentation.wallet.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

@Immutable
internal sealed class WalletEvent {

    data class ChangeWallet(val index: Int) : WalletEvent()

    data class ShowError(val text: TextReference) : WalletEvent()
}
