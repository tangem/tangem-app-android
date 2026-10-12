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
    val transferMessage: String?,
    val depositFee: SerializedBigDecimal?,
    val depositLimit: DepositLimit?,
) {
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
}