package com.tangem.datasource.api.pay.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

/**
 * Response from `GET /v1/customer/offers` — list of offers available to the customer.
 *
 * Used to gate the issue-additional-card flow.
 */
@JsonClass(generateAdapter = true)
data class CustomerOffersResponse(
    @Json(name = "result") val result: List<Offer>,
) {

    @JsonClass(generateAdapter = true)
    data class Offer(
        @Json(name = "type") val type: String,
        @Json(name = "fee") val fee: Fee,
        @Json(name = "data") val data: Data,
    )

    @JsonClass(generateAdapter = true)
    data class Data(
        @Json(name = "specification_name") val specificationName: String,
        @Json(name = "order_type") val orderType: String,
    )

    @JsonClass(generateAdapter = true)
    data class Fee(
        @Json(name = "amount") val amount: BigDecimal,
        @Json(name = "currency") val currency: String,
    )
}