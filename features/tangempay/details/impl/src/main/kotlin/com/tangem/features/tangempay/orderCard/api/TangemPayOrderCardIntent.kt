package com.tangem.features.tangempay.orderCard.api

import kotlinx.serialization.Serializable

@Serializable
internal sealed class TangemPayOrderCardIntent {

    @Serializable
    data object Issue : TangemPayOrderCardIntent()

    @Serializable
    data class ReissuePlastic(
        val sourceProductInstanceId: String,
        val sourceCardId: String,
        val deliveryEtaMaxBusinessDays: Int,
    ) : TangemPayOrderCardIntent()
}