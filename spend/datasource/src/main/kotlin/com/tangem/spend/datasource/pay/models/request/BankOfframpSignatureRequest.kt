package com.tangem.spend.datasource.pay.models.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Body of `v1/order/{order_id}/bank-offramp/signature`: the card's signature over the order's payload hash. */
@Serializable
data class BankOfframpSignatureRequest(
    @SerialName("signature") val signature: String,
)