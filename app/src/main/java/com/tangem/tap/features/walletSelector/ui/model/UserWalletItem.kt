package com.tangem.tap.features.walletSelector.ui.model

import androidx.compose.runtime.Immutable
import com.tangem.tap.features.details.ui.cardsettings.TextReference
import com.tangem.wallet.R

@Immutable
internal sealed interface UserWalletItem {
    val id: String
    val name: String
    val imageUrl: String
    val balance: Balance

    val headerText: TextReference
        get() = when (this) {
            is MultiCurrencyUserWalletItem -> TextReference.Res(R.string.user_wallet_list_multi_header)
            is SingleCurrencyUserWalletItem -> TextReference.Res(R.string.user_wallet_list_single_header)
        }

    @Immutable
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
    val tokensCount: Int,
    val cardsInWallet: Int,
) : UserWalletItem

internal data class SingleCurrencyUserWalletItem(
    override val id: String,
    override val name: String,
    override val imageUrl: String,
    override val balance: UserWalletItem.Balance,
    val tokenName: String,
) : UserWalletItem
