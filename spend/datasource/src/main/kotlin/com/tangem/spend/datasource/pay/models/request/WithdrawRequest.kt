package com.tangem.spend.datasource.pay.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WithdrawRequest(
    @Json(name = "amount_in_cents") val amountInCents: String,
    @Json(name = "recipient_address") val recipientAddress: String,
    @Json(name = "admin_signature") val adminSignature: String,
    @Json(name = "admin_salt") val adminSalt: String,
    @Json(name = "sender_address") val senderAddress: String,
    /** `null` withdraws from the account's default network. */
    @Json(name = "chain_id") val chainId: Long? = null,
    /** Required together with [chainId] for token withdrawals. */
    @Json(name = "token_contract_address") val tokenContractAddress: String? = null,
)