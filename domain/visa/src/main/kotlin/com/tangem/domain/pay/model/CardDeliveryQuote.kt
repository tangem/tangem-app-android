package com.tangem.domain.pay.model

import java.math.BigDecimal
import java.util.Currency

data class CardDeliveryQuote(
    val country: String,
    val isPlasticAvailable: Boolean,
    val isDeliveryFeeWaived: Boolean,
    val deliveryFee: DeliveryFee,
    val deliveryEta: DeliveryEta,
    val hasSufficientBalance: Boolean,
) {

    data class DeliveryFee(
        val amount: BigDecimal,
        val currency: Currency,
    )

    data class DeliveryEta(
        val minBusinessDays: Int,
        val maxBusinessDays: Int,
    )
}