package com.tangem.datasource.api.pay.models.request

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
    )
}