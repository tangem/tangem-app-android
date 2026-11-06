package com.tangem.domain.models.account

import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
data class BankCredentials(
    val type: String,
    val beneficiaryName: String,
    val beneficiaryAddress: String,
    val beneficiaryBankName: String,
    val beneficiaryBankAddress: String,
    val accountNumber: String,
    val routingNumber: String,
    val rail: String?,
    val transferMessage: String?,
    val depositFee: DepositFee?,
    val depositLimit: DepositLimit?,
    val exchangeRate: ExchangeRate? = null,
) {

    val isSpei: Boolean
        get() = rail.equals(SPEI_RAIL, ignoreCase = true)

    val depositCurrencyCode: String
        get() = if (isSpei) MXN_CURRENCY_CODE else USD_CURRENCY_CODE

    @Serializable
    data class DepositFee(
        val amount: SerializedBigDecimal,
        val type: Type,
    ) {
        enum class Type {
            FLAT,
            PERCENTAGE,
            UNKNOWN,
            ;

            companion object {
                fun fromString(value: String) = when (value.uppercase(Locale.US)) {
                    "FLAT" -> FLAT
                    "PERCENTAGE",
                    "VARIABLE",
                    -> PERCENTAGE
                    else -> UNKNOWN
                }
            }
        }
    }

    @Serializable
    data class DepositLimit(
        val amount: SerializedBigDecimal,
        val period: Period,
    ) {
        enum class Period {
            DAY,
            UNKNOWN,
            ;

            companion object {
                fun fromString(value: String) = when (value.uppercase(Locale.US)) {
                    "DAY" -> DAY
                    else -> UNKNOWN
                }
            }
        }
    }

    @Serializable
    data class ExchangeRate(
        val rate: SerializedBigDecimal,
        val base: String,
        val quote: String,
    )

    private companion object {
        const val SPEI_RAIL = "SPEI"
        const val MXN_CURRENCY_CODE = "MXN"
        const val USD_CURRENCY_CODE = "USD"
    }
}