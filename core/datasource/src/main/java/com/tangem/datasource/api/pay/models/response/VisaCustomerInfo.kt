package com.tangem.datasource.api.pay.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VisaCustomerInfo(
    @Json(name = "payment_accounts") val paymentAccounts: List<PaymentAccount>,
) {
    data class PaymentAccount(
        @Json(name = "id") val id: String,
        @Json(name = "customer_wallet_address") val customerWalletAddress: String,
        @Json(name = "payment_account_address") val paymentAccountAddress: String,
    )
}