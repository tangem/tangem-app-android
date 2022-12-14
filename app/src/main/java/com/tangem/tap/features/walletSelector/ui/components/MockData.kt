package com.tangem.tap.features.walletSelector.ui.components

import com.tangem.tap.features.walletSelector.ui.WalletSelectorScreenState
import com.tangem.tap.features.walletSelector.ui.model.MultiCurrencyUserWalletItem
import com.tangem.tap.features.walletSelector.ui.model.SingleCurrencyUserWalletItem
import com.tangem.tap.features.walletSelector.ui.model.UserWalletItem

internal object MockData {
    private val multiCurrencyUserWallet = MultiCurrencyUserWalletItem(
        id = "wallet_1",
        balance = UserWalletItem.Balance(
            amount = "6781.05 $",
            isLoading = false,
        ),
        name = "Wallet",
        imageUrl = "https://app.tangem.com/cards/card_default.png",
        isLocked = false,
        tokensCount = 12,
        cardsInWallet = 3,
    )

    private val singleCurrencyUserWallet = SingleCurrencyUserWalletItem(
        id = "wallet_4",
        balance = UserWalletItem.Balance(
            amount = "6781.05 $",
            isLoading = false,
        ),
        name = "Wallet",
        imageUrl = "https://app.tangem.com/cards/card_default.png",
        isLocked = false,
        tokenName = "Ethereum",
    )

    val state = WalletSelectorScreenState(
        multiCurrencyWallets = listOf(
            multiCurrencyUserWallet,
            multiCurrencyUserWallet.copy(id = "wallet_2"),
            multiCurrencyUserWallet.copy(id = "wallet_3", tokensCount = 2, cardsInWallet = 1),
        ),
        singleCurrencyWallets = listOf(singleCurrencyUserWallet),
        selectedWalletId = multiCurrencyUserWallet.id,
        isLocked = false,
    )
}
