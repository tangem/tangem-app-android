package com.tangem.tap.features.walletSelector.ui.components

import com.tangem.domain.common.util.UserWalletId
import com.tangem.tap.features.walletSelector.ui.WalletSelectorScreenState
import com.tangem.tap.features.walletSelector.ui.model.MultiCurrencyUserWalletItem
import com.tangem.tap.features.walletSelector.ui.model.SingleCurrencyUserWalletItem
import com.tangem.tap.features.walletSelector.ui.model.UserWalletItem

internal object MockData {
    private val multiCurrencyUserWallet = MultiCurrencyUserWalletItem(
        id = UserWalletId("wallet_1"),
        balance = UserWalletItem.Balance.Loaded(
            amount = "6781.05 $",
        ),
        name = "Wallet",
        imageUrl = "https://app.tangem.com/cards/card_default.png",
        isLocked = false,
        tokensCount = 12,
        cardsInWallet = 3,
    )

    private val singleCurrencyUserWallet = SingleCurrencyUserWalletItem(
        id = UserWalletId("wallet_4"),
        balance = UserWalletItem.Balance.Loaded(
            amount = "6781.05 $",
        ),
        name = "Wallet",
        imageUrl = "https://app.tangem.com/cards/card_default.png",
        isLocked = false,
        tokenName = "Ethereum",
    )

    val state = WalletSelectorScreenState(
        multiCurrencyWallets = listOf(
            multiCurrencyUserWallet,
            multiCurrencyUserWallet.copy(id = UserWalletId("wallet_2")),
            multiCurrencyUserWallet.copy(id = UserWalletId("wallet_3"), tokensCount = 2, cardsInWallet = 1),
        ),
        singleCurrencyWallets = listOf(singleCurrencyUserWallet),
        selectedUserWalletId = multiCurrencyUserWallet.id,
        isLocked = false,
    )
}
