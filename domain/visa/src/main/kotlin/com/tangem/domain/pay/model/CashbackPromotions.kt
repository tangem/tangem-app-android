package com.tangem.domain.pay.model

import org.joda.time.DateTime
import java.math.BigDecimal

/** Cashback program configuration for the customer, from `GET v1/customer/cashback/promotions`. */
data class CashbackPromotions(
    val cardTiers: List<CardTier>,
    val additionalCashback: List<AdditionalCashback>,
) {

    data class CardTier(
        val tier: String,
        val label: String,
        val scope: String,
        val minTransactionAmount: BigDecimal?,
        val monthlyCapAmount: BigDecimal?,
    )

    data class AdditionalCashback(
        val id: String,
        val name: String,
        val description: String,
        val isPermanent: Boolean,
        val endDate: DateTime?,
    )
}