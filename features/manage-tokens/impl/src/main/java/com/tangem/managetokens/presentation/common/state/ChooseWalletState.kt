package com.tangem.managetokens.presentation.common.state

import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.managetokens.impl.R
import kotlinx.collections.immutable.ImmutableList

internal sealed class ChooseWalletState {
    data class Choose(
        val wallets: ImmutableList<WalletState>,
        val selectedWallet: WalletState?,
        val onChooseWalletClick: () -> Unit,
        val onCloseChoosingWalletClick: () -> Unit,
        val show: Boolean = false,
    ) : ChooseWalletState()

    object NoSelection : ChooseWalletState()

    class Warning(val type: ChooseWalletWarning) : ChooseWalletState() {
        val message: TextReference
            get() = when (type) {
                ChooseWalletWarning.SINGLE_CURRENCY ->
                    TextReference.Res(R.string.manage_tokens_wallet_support_only_one_network_title)
            }
    }
}

enum class ChooseWalletWarning {
    SINGLE_CURRENCY,
}