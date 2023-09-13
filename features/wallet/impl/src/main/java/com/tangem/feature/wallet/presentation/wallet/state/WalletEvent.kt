package com.tangem.feature.wallet.presentation.wallet.state

import com.tangem.core.ui.extensions.TextReference

sealed class WalletEvent {

    class ChangeWallet(val index: Int) : WalletEvent()

    class ShowError(val text: TextReference) : WalletEvent()
}
