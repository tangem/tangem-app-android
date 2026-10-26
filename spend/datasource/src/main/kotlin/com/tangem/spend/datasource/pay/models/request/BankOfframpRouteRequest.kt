package com.tangem.spend.datasource.pay.models.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BankOfframpRouteRequest(
    @SerialName("product_instance_id") val productInstanceId: String,
    @SerialName("bank_account_id") val bankAccountId: String? = null,
    @SerialName("bank_account") val bankAccount: BankOfframpPlaceOrderRequest.BankAccount? = null,
)