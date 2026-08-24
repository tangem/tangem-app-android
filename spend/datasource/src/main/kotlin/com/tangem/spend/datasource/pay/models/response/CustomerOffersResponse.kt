package com.tangem.spend.datasource.pay.models.response

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
        @Json(name = "images") val images: List<Image> = emptyList(),
    )

    @JsonClass(generateAdapter = true)
    data class Image(
        @Json(name = "type") val type: String?,
        @Json(name = "url") val url: String?,
    )

    @JsonClass(generateAdapter = true)
    data class Data(
        @Json(name = "specification_name") val specificationName: String,
        @Json(name = "order_type") val orderType: String,
        @Json(name = "delivery_eta_min_days") val deliveryEtaMinDays: Int? = null,
        @Json(name = "delivery_eta_max_days") val deliveryEtaMaxDays: Int? = null,
    )

    @JsonClass(generateAdapter = true)
    data class Fee(
        @Json(name = "amount") val amount: BigDecimal,
        @Json(name = "currency") val currency: String,
    )
}