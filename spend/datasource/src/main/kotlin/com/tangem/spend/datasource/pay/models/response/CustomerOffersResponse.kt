package com.tangem.spend.datasource.pay.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

/**
 * Response from `GET /v1/customer/offers` and `GET /v1/product-instances/{id}/offers` — the offers
 * available to the customer, respectively account-wide and for a single product instance.
 */
@JsonClass(generateAdapter = true)
data class CustomerOffersResponse(
    @Json(name = "result") val result: List<Offer>? = null,
) {

    @JsonClass(generateAdapter = true)
    data class Offer(
        @Json(name = "type") val type: String? = null,
        @Json(name = "fee") val fee: Fee? = null,
        @Json(name = "data") val data: Data? = null,
        @Json(name = "images") val images: List<Image>? = null,
    )

    @JsonClass(generateAdapter = true)
    data class Image(
        @Json(name = "type") val type: String?,
        @Json(name = "url") val url: String?,
    )

    @JsonClass(generateAdapter = true)
    data class Data(
        @Json(name = "specification_name") val specificationName: String? = null,
        @Json(name = "order_type") val orderType: String? = null,
        @Json(name = "delivery_eta_min_days") val deliveryEtaMinDays: Int? = null,
        @Json(name = "delivery_eta_max_days") val deliveryEtaMaxDays: Int? = null,
    )

    @JsonClass(generateAdapter = true)
    data class Fee(
        @Json(name = "amount") val amount: BigDecimal? = null,
        @Json(name = "currency") val currency: String? = null,
    )
}