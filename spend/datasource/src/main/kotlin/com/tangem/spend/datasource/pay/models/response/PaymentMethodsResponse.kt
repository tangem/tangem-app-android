package com.tangem.spend.datasource.pay.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

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
        @Json(name = "fees") val fees: Fees?,
    )

    @JsonClass(generateAdapter = true)
    data class Fees(
        @Json(name = "receive") val receive: Fee?,
        @Json(name = "send") val send: Fee?,
    )

    @JsonClass(generateAdapter = true)
    data class Fee(
        @Json(name = "type") val type: String?,
        @Json(name = "calculation_type") val calculationType: String?,
        @Json(name = "amount") val amount: BigDecimal?,
        @Json(name = "currency") val currency: String?,
    )
}