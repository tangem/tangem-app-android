package com.tangem.spend.datasource.pay.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response of `v1/account/bank-credentials/{product_instance_id}`
 */
@Serializable
data class BankCredentialsResponse(
    @SerialName("result") val result: Result?,
) {
    @Serializable
    data class Result(
        @SerialName("type") val type: String?,
        @SerialName("beneficiary_name") val beneficiaryName: String?,
        @SerialName("beneficiary_address") val beneficiaryAddress: String?,
        @SerialName("beneficiary_bank_name") val beneficiaryBankName: String?,
        @SerialName("beneficiary_bank_address") val beneficiaryBankAddress: String?,
        @SerialName("account_number") val accountNumber: String?,
        @SerialName("routing_number") val routingNumber: String?,
        @SerialName("rail") val rail: String?,
        @SerialName("transfer_message") val transferMessage: String?,
        @SerialName("fee") val fee: Fee?,
        @SerialName("deposit_limit") val depositLimit: DepositLimit?,
    )

    @Serializable
    data class Fee(
        @SerialName("type") val type: String?,
        @SerialName("amount") val amount: String?,
    )

    @Serializable
    data class DepositLimit(
        @SerialName("amount") val amount: String?,
        @SerialName("period") val period: String?,
    )
}