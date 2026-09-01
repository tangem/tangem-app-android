package com.tangem.features.polymarket.impl.main.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

@Immutable
internal data class PredictionBalanceUM(
    val amount: AmountUM,
    val addFunds: ActionUM,
    val withdraw: ActionUM,
) {

    @Immutable
    sealed interface AmountUM {

        data object Loading : AmountUM

        data object Unknown : AmountUM

        data class Known(
            val fiat: TextReference,
            val crypto: TextReference,
            val isFlickering: Boolean,
        ) : AmountUM
    }

    /** @property isLoading the account state is not known yet, as opposed to the action being forbidden. */
    @Immutable
    data class ActionUM(
        val isEnabled: Boolean,
        val isLoading: Boolean,
        val onClick: () -> Unit,
    )
}