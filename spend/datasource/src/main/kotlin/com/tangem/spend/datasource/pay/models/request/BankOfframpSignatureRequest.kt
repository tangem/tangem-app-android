package com.tangem.spend.datasource.pay.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Body of `v1/order/{order_id}/bank-offramp/signature`: the card's signature over the order's payload hash. */
@JsonClass(generateAdapter = true)
data class BankOfframpSignatureRequest(
    @Json(name = "signature") val signature: String,
)