package com.tangem.spend.datasource.pay.models.response

import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response from `GET /v1/customer/offers` and `GET /v1/product-instances/{id}/offers` — the offers
 * available to the customer, respectively account-wide and for a single product instance.
 */
@Serializable
data class CustomerOffersResponse(
    @SerialName("result") val result: List<Offer>? = null,
) {

    @Serializable
    data class Offer(
        @SerialName("type") val type: String? = null,
        @SerialName("fee") val fee: Fee? = null,
        @SerialName("data") val data: Data? = null,
        @SerialName("images") val images: List<Image>? = null,
    )

    @Serializable
    data class Image(
        @SerialName("type") val type: String?,
        @SerialName("url") val url: String?,
    )

    @Serializable
    data class Data(
        @SerialName("specification_name") val specificationName: String? = null,
        @SerialName("order_type") val orderType: String? = null,
        @SerialName("delivery_eta_min_days") val deliveryEtaMinDays: Int? = null,
        @SerialName("delivery_eta_max_days") val deliveryEtaMaxDays: Int? = null,
    )

    @Serializable
    data class Fee(
        @SerialName("amount") val amount: SerializedBigDecimal? = null,
        @SerialName("currency") val currency: String? = null,
    )
}