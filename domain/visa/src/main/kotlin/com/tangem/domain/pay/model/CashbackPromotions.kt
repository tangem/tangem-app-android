package com.tangem.domain.pay.model

import java.math.BigDecimal

/** Cashback program configuration for the customer, from `GET v1/customer/cashback/promotions`. */
data class CashbackPromotions(
    val cardTiers: List<CardTier>,
) {

    data class CardTier(
        val tier: String,
        val label: String,
        val scope: String,
        val minTransactionAmount: BigDecimal?,
        val monthlyCapAmount: BigDecimal?,
    )
}