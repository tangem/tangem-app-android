package com.tangem.datasource.api.pay.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Response of `bff-v2/v1/account/bank-credentials/{product_instance_id}` — fiat bank requisites for the
 * Virtual Account on-ramp (VA MVP0, TWI-1638).
 */
@JsonClass(generateAdapter = true)
data class BankCredentialsResponse(
    @Json(name = "type") val type: String?,
    @Json(name = "beneficiary_name") val beneficiaryName: String?,
    @Json(name = "beneficiary_address") val beneficiaryAddress: String?,
    @Json(name = "beneficiary_bank_name") val beneficiaryBankName: String?,
    @Json(name = "beneficiary_bank_address") val beneficiaryBankAddress: String?,
    @Json(name = "account_number") val accountNumber: String?,
    @Json(name = "routing_number") val routingNumber: String?,
)