package com.tangem.features.collectibles.api.walletblock

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

/** State of the Collectibles account row. The balance arrives formatted: formatting needs the app currency. */
@Immutable
sealed class CollectiblesWalletBlockUM {

    data object Hidden : CollectiblesWalletBlockUM()

    /** The owner key is not derived on this device yet: the row stays inert until the wallet's addresses are synced. */
    data object SyncNeeded : CollectiblesWalletBlockUM()

    data class Content(
        val balance: Balance,
        val onClick: () -> Unit,
    ) : CollectiblesWalletBlockUM()

    @Immutable
    sealed class Balance {

        /** The account record is being read for the first time — shimmers, like the wallet's own account rows. */
        data object Loading : Balance()

        /** No amount to show and none coming. */
        data object Unknown : Balance()

        data class Amount(val text: TextReference) : Balance()
    }
}