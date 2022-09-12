package com.tangem.tap.features.userWalletsList.ui

import androidx.compose.runtime.Immutable
import com.tangem.common.core.TangemError
import com.tangem.tap.common.entities.FiatCurrency
import java.math.BigDecimal

@Immutable
data class WalletSelectorScreenState(
    val wallets: List<UserWallet> = listOf(),
    val currentWalletId: String? = null,
    val fiatCurrency: FiatCurrency = FiatCurrency.Default,
    val isLocked: Boolean = true,
    val selectedWalletIds: List<String> = listOf(),
    val error: TangemError? = null,
) {
    @Immutable
    data class UserWallet(
        val id: String,
        val name: String,
        val imageUrl: String,
        val amount: BigDecimal,
        val type: Type,
    ) {
        sealed interface Type {
            class MultiCurrency(
                val tokensCount: Int,
                val cardsInWallet: Int,
            ) : Type

            class SingleCurrency(val tokenName: String) : Type
        }
    }
}
