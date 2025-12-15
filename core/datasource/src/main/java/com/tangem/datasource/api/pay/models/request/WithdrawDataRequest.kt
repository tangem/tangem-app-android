package com.tangem.datasource.api.pay.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WithdrawDataRequest(
    @Json(name = "amount_in_cents") val amountInCents: String,
    @Json(name = "recipient_address") val recipientAddress: String,
)