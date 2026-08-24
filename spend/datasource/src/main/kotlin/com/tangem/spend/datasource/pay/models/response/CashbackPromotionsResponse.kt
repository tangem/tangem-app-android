package com.tangem.spend.datasource.pay.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

/**
 * Response from `GET v1/customer/cashback/promotions`
 */
@JsonClass(generateAdapter = true)
data class CashbackPromotionsResponse(
    @Json(name = "result") val result: Result?,
) {

    @JsonClass(generateAdapter = true)
    data class Result(
        @Json(name = "cashback_on_cards") val cashbackOnCards: CashbackOnCards?,
        @Json(name = "additional_cashback") val additionalCashback: List<AdditionalCashback>?,
    )

    @JsonClass(generateAdapter = true)
    data class CashbackOnCards(
        @Json(name = "cards") val cards: List<Card>?,
        @Json(name = "account_monthly_cap_amount") val accountMonthlyCapAmount: BigDecimal?,
        @Json(name = "account_monthly_cap_currency") val accountMonthlyCapCurrency: String?,
    )

    @JsonClass(generateAdapter = true)
    data class Card(
        @Json(name = "card_type") val cardType: String,
        @Json(name = "title") val title: String?,
        @Json(name = "card_cashback_rate") val cardCashbackRate: BigDecimal,
        @Json(name = "min_transaction_amount") val minTransactionAmount: BigDecimal?,
        @Json(name = "promotion_id") val promotionId: String,
    )

    @JsonClass(generateAdapter = true)
    data class AdditionalCashback(
        @Json(name = "id") val id: String,
        @Json(name = "card_type") val cardType: String?,
        @Json(name = "name") val name: String,
        @Json(name = "description") val description: String?,
        @Json(name = "end_date") val endDate: String?,
        @Json(name = "promo_cap_amount") val promoCapAmount: BigDecimal?,
        @Json(name = "promo_cap_period") val promoCapPeriod: String?,
        @Json(name = "cap_currency") val capCurrency: String?,
        @Json(name = "min_transaction_amount") val minTransactionAmount: BigDecimal?,
        @Json(name = "priority") val priority: Int,
    )
}