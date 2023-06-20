package com.tangem.tap.features.walletSelector.ui.components

import com.tangem.domain.models.userwallet.UserWalletId
import com.tangem.tap.features.walletSelector.ui.WalletSelectorScreenState
import com.tangem.tap.features.walletSelector.ui.model.MultiCurrencyUserWalletItem
import com.tangem.tap.features.walletSelector.ui.model.SingleCurrencyUserWalletItem
import com.tangem.tap.features.walletSelector.ui.model.UserWalletItem

internal object MockData {
    private val multiCurrencyUserWallet = MultiCurrencyUserWalletItem(
        id = UserWalletId.mock(n = 1),
        balance = UserWalletItem.Balance.Loaded(
            amount = "6781.05 $",
            showWarning = true,
        ),
        name = "Wallet",
        imageUrl = "https://app.tangem.com/cards/card_default.png",
        isLocked = false,
        tokensCount = 12,
        cardsInWallet = 3,
    )

    private val singleCurrencyUserWallet = SingleCurrencyUserWalletItem(
        id = UserWalletId.mock(n = 4),
        balance = UserWalletItem.Balance.Loaded(
            amount = "6781.05 $",
            showWarning = false,
        ),
        name = "Wallet",
        imageUrl = "https://app.tangem.com/cards/card_default.png",
        isLocked = false,
        tokenName = "Ethereum",
    )

    val state = WalletSelectorScreenState(
        multiCurrencyWallets = listOf(
            multiCurrencyUserWallet,
            multiCurrencyUserWallet.copy(id = UserWalletId.mock(n = 2)),
            multiCurrencyUserWallet.copy(id = UserWalletId.mock(n = 4), tokensCount = 2, cardsInWallet = 1),
        ),
        singleCurrencyWallets = listOf(singleCurrencyUserWallet),
        selectedUserWalletId = multiCurrencyUserWallet.id,
        isLocked = false,
    )
}
