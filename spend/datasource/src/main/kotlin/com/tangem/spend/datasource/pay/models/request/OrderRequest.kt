package com.tangem.spend.datasource.pay.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OrderRequest(
    @Json(name = "data") val data: Data,
    @Json(name = "idempotency_key") val idempotencyKey: String,
) {
    @JsonClass(generateAdapter = true)
    data class Data(
        @Json(name = "customer_wallet_address") val customerWalletAddress: String,
        @Json(name = "specification_name") val specificationName: String?,
        @Json(name = "type") val type: String,
        @Json(name = "target_tariff_plan_id") val targetTariffPlanId: String? = null,
        @Json(name = "tariff_plan_transition_type") val tariffPlanTransitionType: String? = null,
        @Json(name = "chain_id") val chainId: Int? = null,
        @Json(name = "emboss_name") val embossName: String? = null,
        @Json(name = "shipping_address") val shippingAddress: ShippingAddress? = null,
        @Json(name = "product_instance_id") val productInstanceId: String? = null,
        @Json(name = "source_product_instance_id") val sourceProductInstanceId: String? = null,
        @Json(name = "last_four_digits") val lastFourDigits: String? = null,
    )

    @JsonClass(generateAdapter = true)
    data class ShippingAddress(
        @Json(name = "first_name") val firstName: String,
        @Json(name = "last_name") val lastName: String,
        @Json(name = "line1") val line1: String,
        @Json(name = "line2") val line2: String?,
        @Json(name = "city") val city: String,
        @Json(name = "region") val region: String,
        @Json(name = "postal_code") val postalCode: String,
        @Json(name = "phone_number") val phoneNumber: String,
    )
}