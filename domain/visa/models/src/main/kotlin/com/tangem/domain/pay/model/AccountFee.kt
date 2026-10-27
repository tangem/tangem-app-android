package com.tangem.domain.pay.model

import java.math.BigDecimal
import java.util.Locale

data class AccountFee(
    val type: String,
    val name: String,
    val paymentMethodId: String?,
    val group: AccountFeeGroupType,
    val calculationType: CalculationType,
    val amount: BigDecimal,
    val currency: String,
) {
    enum class CalculationType {
        FIXED,
        PERCENTAGE,
        ;

        companion object {
            fun fromString(value: String?): CalculationType = when (value?.uppercase(Locale.US)) {
                "PERCENTAGE" -> PERCENTAGE
                else -> FIXED
            }
        }
    }
}