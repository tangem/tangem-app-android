package com.tangem.domain.pay.model

import org.joda.time.DateTime
import java.math.BigDecimal

data class CashbackPromotions(
    val cards: List<CardPromotion>,
    val accountMonthlyCap: MonthlyCap?,
    val additionalCashback: List<AdditionalCashback>,
) {

    data class CardPromotion(
        val cardType: String,
        val title: String?,
        val cashbackRate: BigDecimal,
        val minTransactionAmount: BigDecimal?,
        val promotionId: String,
    )

    data class MonthlyCap(
        val amount: BigDecimal,
        val currency: String?,
    )

    data class AdditionalCashback(
        val id: String,
        val cardType: String?,
        val name: String,
        val description: String?,
        val endDate: DateTime?,
        val promoCap: PromoCap?,
        val minTransactionAmount: BigDecimal?,
        val priority: Int,
    )

    data class PromoCap(
        val amount: BigDecimal,
        val period: Period,
        val currency: String?,
    ) {
        enum class Period {
            MONTHLY,
            UNKNOWN,
            ;

            companion object {
                fun fromString(value: String?): Period = when (value?.lowercase()) {
                    "monthly" -> MONTHLY
                    else -> UNKNOWN
                }
            }
        }
    }
}