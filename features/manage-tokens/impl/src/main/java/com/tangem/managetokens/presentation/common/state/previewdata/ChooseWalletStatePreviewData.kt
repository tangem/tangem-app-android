package com.tangem.managetokens.presentation.common.state.previewdata

import com.tangem.managetokens.presentation.common.state.ChooseWalletState
import com.tangem.managetokens.presentation.common.state.WalletState
import kotlinx.collections.immutable.persistentListOf

internal object ChooseWalletStatePreviewData {

    val state: ChooseWalletState.Choose
        get() = ChooseWalletState.Choose(
            wallets = persistentListOf(
                walletState,
                walletState.copy(walletId = "2"),
            ),
            selectedWallet = walletState,
            onChooseWalletClick = {},
            onCloseChoosingWalletClick = {},
        )

    private val walletState: WalletState
        get() = WalletState(
            walletName = "My wallet",
            walletId = "1",
            artworkUrl = "",
            onSelected = {},
        )
}
