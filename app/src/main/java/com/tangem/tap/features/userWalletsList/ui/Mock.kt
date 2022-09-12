package com.tangem.tap.features.userWalletsList.ui

import com.tangem.tap.features.userWalletsList.ui.WalletSelectorScreenState.UserWallet
import java.math.BigDecimal

object Mock {
    private val userWallet = UserWallet(
        id = "wallet_1",
        amount = BigDecimal("6781.05"),
        name = "Wallet",
        imageUrl = "https://app.tangem.com/cards/card_default.png",
        type = UserWallet.Type.MultiCurrency(tokensCount = 12, cardsInWallet = 3),
    )

    val userWalletList = listOf(
        userWallet,
        userWallet.copy(id = "wallet_2"),
        userWallet.copy(id = "wallet_3", type = UserWallet.Type.MultiCurrency(tokensCount = 2, cardsInWallet = 1)),
        userWallet.copy(id = "wallet_4", type = UserWallet.Type.SingleCurrency(tokenName = "Ethereum")),
    )

    val state = WalletSelectorScreenState(
        wallets = userWalletList,
        currentWalletId = userWalletList[0].id,
        isLocked = true,
    )
}
