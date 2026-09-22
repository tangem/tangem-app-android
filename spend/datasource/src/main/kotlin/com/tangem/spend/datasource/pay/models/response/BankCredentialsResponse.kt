package com.tangem.spend.datasource.pay.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Response of `v1/account/bank-credentials/{product_instance_id}`
 */
@JsonClass(generateAdapter = true)
data class BankCredentialsResponse(
    @Json(name = "result") val result: Result?,
) {
    @JsonClass(generateAdapter = true)
    data class Result(
        @Json(name = "type") val type: String?,
        @Json(name = "beneficiary_name") val beneficiaryName: String?,
        @Json(name = "beneficiary_address") val beneficiaryAddress: String?,
        @Json(name = "beneficiary_bank_name") val beneficiaryBankName: String?,
        @Json(name = "beneficiary_bank_address") val beneficiaryBankAddress: String?,
        @Json(name = "account_number") val accountNumber: String?,
        @Json(name = "routing_number") val routingNumber: String?,
        @Json(name = "rail") val rail: String?,
        @Json(name = "transfer_message") val transferMessage: String?,
        @Json(name = "fee") val fee: Fee?,
        @Json(name = "deposit_limit") val depositLimit: DepositLimit?,
    )

    @JsonClass(generateAdapter = true)
    data class Fee(
        @Json(name = "type") val type: String?,
        @Json(name = "amount") val amount: String?,
    )

    @JsonClass(generateAdapter = true)
    data class DepositLimit(
        @Json(name = "amount") val amount: String?,
        @Json(name = "period") val period: String?,
    )
}