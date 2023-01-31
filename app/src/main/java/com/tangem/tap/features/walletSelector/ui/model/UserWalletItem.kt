package com.tangem.tap.features.walletSelector.ui.model

import com.tangem.domain.common.util.UserWalletId

internal sealed interface UserWalletItem {
    val id: UserWalletId
    val name: String
    val imageUrl: String
    val balance: Balance
    val isLocked: Boolean

    sealed interface Balance {
        object Loading : Balance

        data class Error(val amount: String) : Balance

        data class Loaded(val amount: String) : Balance
    }
}

internal data class MultiCurrencyUserWalletItem(
    override val id: UserWalletId,
    override val name: String,
    override val imageUrl: String,
    override val balance: UserWalletItem.Balance,
    override val isLocked: Boolean,
    val tokensCount: Int,
    val cardsInWallet: Int,
) : UserWalletItem

internal data class SingleCurrencyUserWalletItem(
    override val id: UserWalletId,
    override val name: String,
    override val imageUrl: String,
    override val balance: UserWalletItem.Balance,
    override val isLocked: Boolean,
    val tokenName: String,
) : UserWalletItem
