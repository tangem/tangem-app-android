package com.tangem.tap.features.walletSelector.ui.model

import androidx.compose.runtime.Immutable
import com.tangem.tap.features.details.ui.cardsettings.TextReference
import com.tangem.wallet.R

@Immutable
internal data class UserWalletItem(
    val id: String,
    val name: String,
    val imageUrl: String,
    val balance: Balance,
    val type: Type,
) {
    @Immutable
    data class Balance(
        val amount: String,
        val isLoading: Boolean,
    ) {
        companion object {
            val Loading = Balance(
                amount = "0.00",
                isLoading = true,
            )
        }
    }

    sealed interface Type {
        val headerText: TextReference

        @Immutable
        class MultiCurrency(
            val tokensCount: Int,
            val cardsInWallet: Int,
        ) : Type {
            override val headerText: TextReference
                get() = TextReference.Res(R.string.user_wallet_list_multi_header)
        }

        @Immutable
        class SingleCurrency(val tokenName: String) : Type {
            override val headerText: TextReference
                get() = TextReference.Res(R.string.user_wallet_list_single_header)
        }
    }
}