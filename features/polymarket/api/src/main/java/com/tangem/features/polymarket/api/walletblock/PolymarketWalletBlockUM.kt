package com.tangem.features.polymarket.api.walletblock

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

/** State of the Prediction account row. The balance arrives formatted: formatting needs the app currency. */
@Immutable
sealed class PolymarketWalletBlockUM {

    data object Hidden : PolymarketWalletBlockUM()

    /**
     * @property isBalanceFlickering a refresh is in flight
     * @property isBalanceFromCache the refresh failed and the balance shown is the last one known
     */
    data class Content(
        val balance: Balance,
        val isBalanceFlickering: Boolean,
        val isBalanceFromCache: Boolean,
        val onClick: () -> Unit,
    ) : PolymarketWalletBlockUM()

    @Immutable
    sealed class Balance {

        /** Being fetched for the first time — shimmers, like the wallet's own account rows. */
        data object Loading : Balance()

        /** No amount to show and none coming: onboarding unfinished, no credentials here, no quote. */
        data object Unknown : Balance()

        data class Amount(val text: TextReference) : Balance()
    }
}