package com.tangem.spend.datasource.pay.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

/**
 * Response from `GET v1/customer/cashback/promotions` — cashback program configuration for the customer.
 */
@JsonClass(generateAdapter = true)
data class CashbackPromotionsResponse(
    @Json(name = "cashback_on_cards") val cashbackOnCards: CashbackOnCards?,
    @Json(name = "additional_cashback") val additionalCashback: List<AdditionalCashback>?,
) {

    @JsonClass(generateAdapter = true)
    data class CashbackOnCards(
        @Json(name = "tiers") val tiers: List<CardTier>?,
        @Json(name = "monthly_cap_amount") val monthlyCapAmount: BigDecimal?,
        @Json(name = "monthly_cap_currency") val monthlyCapCurrency: String?,
    )

    @JsonClass(generateAdapter = true)
    data class CardTier(
        @Json(name = "tier") val tier: String?,
        @Json(name = "label") val label: String?,
        @Json(name = "scope") val scope: String?,
        @Json(name = "min_transaction_amount") val minTransactionAmount: BigDecimal?,
        @Json(name = "tier_monthly_cap_amount") val tierMonthlyCapAmount: BigDecimal?,
        @Json(name = "promotion_id") val promotionId: String?,
    )

    @JsonClass(generateAdapter = true)
    data class AdditionalCashback(
        @Json(name = "id") val id: String?,
        @Json(name = "name") val name: String?,
        @Json(name = "description") val description: String?,
        @Json(name = "is_permanent") val isPermanent: Boolean?,
        @Json(name = "end_date") val endDate: String?,
    )
}