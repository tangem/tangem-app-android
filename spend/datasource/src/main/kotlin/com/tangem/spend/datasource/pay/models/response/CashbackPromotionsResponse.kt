package com.tangem.spend.datasource.pay.models.response

import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response from `GET v1/customer/cashback/promotions`
 */
@Serializable
data class CashbackPromotionsResponse(
    @SerialName("result") val result: Result?,
) {

    @Serializable
    data class Result(
        @SerialName("cashback_on_cards") val cashbackOnCards: CashbackOnCards?,
        @SerialName("additional_cashback") val additionalCashback: List<AdditionalCashback>?,
    )

    @Serializable
    data class CashbackOnCards(
        @SerialName("cards") val cards: List<Card>?,
        @SerialName("account_monthly_cap_amount") val accountMonthlyCapAmount: SerializedBigDecimal?,
        @SerialName("account_monthly_cap_currency") val accountMonthlyCapCurrency: String?,
    )

    @Serializable
    data class Card(
        @SerialName("card_type") val cardType: String,
        @SerialName("title") val title: String?,
        @SerialName("card_cashback_rate") val cardCashbackRate: SerializedBigDecimal,
        @SerialName("min_transaction_amount") val minTransactionAmount: SerializedBigDecimal?,
        @SerialName("promotion_id") val promotionId: String,
    )

    @Serializable
    data class AdditionalCashback(
        @SerialName("id") val id: String,
        @SerialName("card_type") val cardType: String?,
        @SerialName("name") val name: String,
        @SerialName("description") val description: String?,
        @SerialName("end_date") val endDate: String?,
        @SerialName("promo_cap_amount") val promoCapAmount: SerializedBigDecimal?,
        @SerialName("promo_cap_period") val promoCapPeriod: String?,
        @SerialName("cap_currency") val capCurrency: String?,
        @SerialName("min_transaction_amount") val minTransactionAmount: SerializedBigDecimal?,
        @SerialName("priority") val priority: Int,
    )
}