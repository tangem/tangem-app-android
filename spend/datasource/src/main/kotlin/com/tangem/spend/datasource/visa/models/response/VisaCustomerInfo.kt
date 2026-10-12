package com.tangem.spend.datasource.visa.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VisaCustomerInfo(
    @SerialName("payment_accounts") val paymentAccounts: List<PaymentAccount>,
) {
    @Serializable
    data class PaymentAccount(
        @SerialName("id") val id: String,
        @SerialName("customer_wallet_address") val customerWalletAddress: String,
        @SerialName("payment_account_address") val paymentAccountAddress: String,
    )
}