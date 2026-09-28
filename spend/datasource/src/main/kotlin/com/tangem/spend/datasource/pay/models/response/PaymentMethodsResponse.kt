package com.tangem.spend.datasource.pay.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Response of `bff-v2/v1/payment-methods`
 */
@JsonClass(generateAdapter = true)
data class PaymentMethodsResponse(
    @Json(name = "result") val result: List<PaymentMethod>?,
) {

    @JsonClass(generateAdapter = true)
    data class PaymentMethod(
        @Json(name = "id") val id: String?,
        @Json(name = "name") val name: String?,
        @Json(name = "country_code") val countryCode: String?,
        @Json(name = "icon_url") val iconUrl: String?,
    )
}