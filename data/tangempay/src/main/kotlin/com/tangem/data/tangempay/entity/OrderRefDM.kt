package com.tangem.data.tangempay.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OrderRefDM(
    @SerialName("order_id") val orderId: String,
)