package com.tangem.features.onramp.main.entity

import com.tangem.domain.onramp.model.OnrampPaymentMethod

sealed interface OnrampProvidersUM {

    data object Empty : OnrampProvidersUM

    data object Loading : OnrampProvidersUM

    data class Content(
        val providerId: String,
        val paymentMethod: OnrampPaymentMethod,
    ) : OnrampProvidersUM
}