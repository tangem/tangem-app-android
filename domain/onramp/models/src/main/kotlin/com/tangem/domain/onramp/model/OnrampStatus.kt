package com.tangem.domain.onramp.model

import kotlinx.serialization.Serializable

@Serializable
data class OnrampStatus(
    val txId: String,
    val providerId: String,
    val payoutAddress: String,
    val status: Status,
    val failReason: String?,
    val externalTxId: String,
    val externalTxUrl: String?,
    val payoutHash: String?,
    val createdAt: String,
    val fromCurrencyCode: String,
    val fromAmount: String,
    val toContractAddress: String,
    val toNetwork: String,
    val toDecimals: String,
    val toAmount: String?,
    val toActualAmount: String?,
    val paymentMethod: String,
    val countryCode: String,
) {
    enum class Status {
        Created,
        Expired,
        Paused,
        WaitingForPayment,
        PaymentProcessing,
        Verifying,
        Failed,
        Paid,
        Sending,
        Finished,
    }
}
