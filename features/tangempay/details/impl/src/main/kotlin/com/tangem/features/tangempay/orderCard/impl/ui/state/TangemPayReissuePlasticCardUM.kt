package com.tangem.features.tangempay.orderCard.impl.ui.state

import androidx.compose.runtime.Immutable

@Immutable
internal sealed class TangemPayReissuePlasticCardUM {

    abstract val onDismissRequest: () -> Unit

    @Immutable
    data class Loading(override val onDismissRequest: () -> Unit) : TangemPayReissuePlasticCardUM()

    @Immutable
    data class Error(
        override val onDismissRequest: () -> Unit,
        val onRetry: () -> Unit,
    ) : TangemPayReissuePlasticCardUM()

    @Immutable
    data class Content(
        override val onDismissRequest: () -> Unit,
        val country: String,
        val deliveryFee: String,
        val deliveryEtaMaxBusinessDays: Int,
        val isInsufficientFunds: Boolean,
        val onReplaceClick: () -> Unit,
    ) : TangemPayReissuePlasticCardUM() {

        val isReplaceEnabled: Boolean get() = !isInsufficientFunds
    }
}