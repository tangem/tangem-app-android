package com.tangem.domain.pay.model

import java.math.BigDecimal
import java.util.Locale

data class TangemPayCardLimit(
    val amount: BigDecimal,
    val period: TangemPayCardLimitPeriod,
)

enum class TangemPayCardLimitPeriod {
    DAY,
    WEEK,
    MONTH,
    YEAR,
    ALL_TIME,
    AUTHORIZATION,
    UNKNOWN,
    ;

    companion object {
        fun fromString(value: String) = when (value.uppercase(Locale.US)) {
            "DAY" -> DAY
            "WEEK" -> WEEK
            "MONTH" -> MONTH
            "YEAR" -> YEAR
            "ALL_TIME" -> ALL_TIME
            "AUTHORIZATION" -> AUTHORIZATION
            else -> UNKNOWN
        }
    }
}