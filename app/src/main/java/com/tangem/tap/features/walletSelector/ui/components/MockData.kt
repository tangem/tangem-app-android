package com.tangem.tap.features.walletSelector.ui.components

import com.tangem.tap.features.walletSelector.ui.WalletSelectorScreenState
import com.tangem.tap.features.walletSelector.ui.model.UserWalletItem

internal object MockData {
    private val userWallet = UserWalletItem(
        id = "wallet_1",
        balance = UserWalletItem.Balance(
            amount = "6781.05 $",
            isLoading = false,
        ),
        name = "Wallet",
        imageUrl = "https://app.tangem.com/cards/card_default.png",
        type = UserWalletItem.Type.MultiCurrency(tokensCount = 12, cardsInWallet = 3),
    )

    val userWalletList = listOf(
        userWallet,
        userWallet.copy(id = "wallet_2"),
        userWallet.copy(id = "wallet_3", type = UserWalletItem.Type.MultiCurrency(tokensCount = 2, cardsInWallet = 1)),
        userWallet.copy(id = "wallet_4", type = UserWalletItem.Type.SingleCurrency(tokenName = "Ethereum")),
    )

    val state = WalletSelectorScreenState(
        wallets = userWalletList,
        selectedWalletId = userWalletList[0].id,
        isLocked = false,
    )
}