package com.tangem.tap.features.walletSelector.redux

import com.tangem.domain.common.util.UserWalletId
import com.tangem.tap.domain.model.TotalFiatBalance

data class UserWalletModel(
    val id: UserWalletId,
    val name: String,
    val artworkUrl: String,
    val type: Type,
    val fiatBalance: TotalFiatBalance,
    val isLocked: Boolean,
) {
    sealed interface Type {
        data class SingleCurrency(
            val blockchainName: String,
        ) : Type

        data class MultiCurrency(
            val cardsInWallet: Int,
            val tokensCount: Int,
        ) : Type
    }
}
