package com.tangem.features.onramp.main.entity

sealed class OnrampProviderBlockUM {
    data object Empty : OnrampProviderBlockUM()
    data object Loading : OnrampProviderBlockUM()
    data class Content(
        val paymentMethodIconUrl: String,
        val paymentMethodName: String,
        val providerName: String,
        val isBestRate: Boolean,
        val onClick: () -> Unit,
    ) : OnrampProviderBlockUM()
}
