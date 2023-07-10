package com.tangem.tap.features.walletSelector.ui.model

import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.tap.features.wallet.redux.utils.UNKNOWN_AMOUNT_SIGN

internal sealed interface UserWalletItem {
    val id: UserWalletId
    val name: String
    val imageUrl: String
    val balance: Balance
    val isLocked: Boolean

    sealed class Balance {
        open val amount: String = UNKNOWN_AMOUNT_SIGN
        open val showWarning: Boolean = false

        object Loading : Balance()

        object Failed : Balance() {
            override val showWarning: Boolean = true
        }

        data class Loaded(
            override val amount: String,
            override val showWarning: Boolean,
        ) : Balance()
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