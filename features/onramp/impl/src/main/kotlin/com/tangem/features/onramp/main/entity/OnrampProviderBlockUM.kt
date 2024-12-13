package com.tangem.features.onramp.main.entity

import com.tangem.domain.onramp.model.OnrampPaymentMethod

sealed class OnrampProviderBlockUM {
    data object Empty : OnrampProviderBlockUM()
    data object Loading : OnrampProviderBlockUM()
    data class Content(
        val providerId: String,
        val paymentMethod: OnrampPaymentMethod,
        val providerName: String,
        val termsOfUseLink: String?,
        val privacyPolicyLink: String?,
        val isBestRate: Boolean,
        val onLinkClick: (String) -> Unit,
        val onClick: () -> Unit,
    ) : OnrampProviderBlockUM()
}