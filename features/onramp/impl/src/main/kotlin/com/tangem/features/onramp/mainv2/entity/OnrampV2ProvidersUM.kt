package com.tangem.features.onramp.mainv2.entity

import com.tangem.domain.onramp.model.OnrampPaymentMethod

sealed interface OnrampV2ProvidersUM {

    data object Empty : OnrampV2ProvidersUM

    data object Loading : OnrampV2ProvidersUM

    data class Content(
        val providerId: String,
        val paymentMethod: OnrampPaymentMethod,
    ) : OnrampV2ProvidersUM
}