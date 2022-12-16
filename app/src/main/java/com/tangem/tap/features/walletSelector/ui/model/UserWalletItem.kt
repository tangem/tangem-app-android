package com.tangem.tap.features.walletSelector.ui.model

internal sealed interface UserWalletItem {
    val id: String
    val name: String
    val imageUrl: String
    val balance: Balance
    val isLocked: Boolean

    data class Balance(
        val amount: String,
        val isLoading: Boolean,
    )
}

internal data class MultiCurrencyUserWalletItem(
    override val id: String,
    override val name: String,
    override val imageUrl: String,
    override val balance: UserWalletItem.Balance,
    override val isLocked: Boolean,
    val tokensCount: Int,
    val cardsInWallet: Int,
) : UserWalletItem

internal data class SingleCurrencyUserWalletItem(
    override val id: String,
    override val name: String,
    override val imageUrl: String,
    override val balance: UserWalletItem.Balance,
    override val isLocked: Boolean,
    val tokenName: String,
) : UserWalletItem
