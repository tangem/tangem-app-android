package com.tangem.spend.datasource.pay.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
data class CardDeliveryQuoteResponse(
    @Json(name = "country") val country: String,
    @Json(name = "plastic_available") val isPlasticAvailable: Boolean,
    @Json(name = "delivery_fee_waived") val isDeliveryFeeWaived: Boolean,
    @Json(name = "delivery_fee") val deliveryFee: DeliveryFee,
    @Json(name = "delivery_eta") val deliveryEta: DeliveryEta,
    @Json(name = "sufficient_balance") val hasSufficientBalance: Boolean,
) {

    @JsonClass(generateAdapter = true)
    data class DeliveryFee(
        @Json(name = "amount") val amount: BigDecimal,
        @Json(name = "currency") val currency: String,
    )

    @JsonClass(generateAdapter = true)
    data class DeliveryEta(
        @Json(name = "min_business_days") val minBusinessDays: Int,
        @Json(name = "max_business_days") val maxBusinessDays: Int,
    )
}