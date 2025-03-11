package com.tangem.datasource.api.visa.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SetPinCodeRequest(
    @Json(name = "customer_id") val customerId: String,
    @Json(name = "activation_order_id") val activationOrderId: String,
    @Json(name = "product_instance_id") val productInstanceId: String,
    @Json(name = "data") val data: Data,
) {
    data class Data(
        @Json(name = "session_key") val sessionKey: String,
        @Json(name = "iv") val iv: String,
        @Json(name = "encrypted_pin") val encryptedPin: String,
    )
}