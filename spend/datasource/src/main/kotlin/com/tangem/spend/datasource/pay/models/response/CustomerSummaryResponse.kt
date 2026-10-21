package com.tangem.spend.datasource.pay.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response from `GET /v1/customer/summary`.
 */
@Serializable
data class CustomerSummaryResponse(
    @SerialName("result") val result: Result?,
) {
    @Serializable
    data class Result(
        @SerialName("customer_id") val customerId: String,
        @SerialName("customer_status") val customerStatus: String,
        @SerialName("hidden") val isHidden: Boolean,
        @SerialName("kyc_status") val kycStatus: String,
        @SerialName("payment_account") val paymentAccount: PaymentAccount,
        @SerialName("banking_account") val bankingAccount: BankingAccount,
    )

    @Serializable
    data class PaymentAccount(
        @SerialName("id") val id: String?,
        @SerialName("activation") val activation: Activation,
        @SerialName("cards") val cards: Cards,
    )

    @Serializable
    data class BankingAccount(
        @SerialName("id") val id: String?,
        @SerialName("activation") val activation: Activation,
    )

    @Serializable
    data class Activation(
        @SerialName("status") val status: String,
        @SerialName("step") val step: String?,
        @SerialName("failure_reason") val failureReason: String?,
    )

    @Serializable
    data class Cards(
        @SerialName("active") val active: Int,
        @SerialName("frozen") val frozen: Int,
        @SerialName("pending") val pending: Int,
    )
}