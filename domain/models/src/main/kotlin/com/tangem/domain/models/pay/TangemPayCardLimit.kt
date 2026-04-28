package com.tangem.domain.models.pay

import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
data class TangemPayCardLimit(
    @SerialName("amount") val amount: SerializedBigDecimal,
    @SerialName("period") val period: TangemPayCardLimitPeriod,
)

@Serializable
enum class TangemPayCardLimitPeriod {
    @SerialName("DAY")
    DAY,

    @SerialName("WEEK")
    WEEK,

    @SerialName("MONTH")
    MONTH,

    @SerialName("YEAR")
    YEAR,

    @SerialName("ALL_TIME")
    ALL_TIME,

    @SerialName("AUTHORIZATION")
    AUTHORIZATION,

    @SerialName("UNKNOWN")
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