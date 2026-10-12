package com.tangem.spend.datasource.pay.models.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WithdrawRequest(
    @SerialName("amount_in_cents") val amountInCents: String,
    @SerialName("recipient_address") val recipientAddress: String,
    @SerialName("admin_signature") val adminSignature: String,
    @SerialName("admin_salt") val adminSalt: String,
    @SerialName("sender_address") val senderAddress: String,
    /** `null` withdraws from the account's default network. */
    @SerialName("chain_id") val chainId: Long? = null,
    /** Required together with [chainId] for token withdrawals. */
    @SerialName("token_contract_address") val tokenContractAddress: String? = null,
)