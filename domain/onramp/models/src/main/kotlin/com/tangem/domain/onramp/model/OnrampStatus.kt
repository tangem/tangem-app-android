package com.tangem.domain.onramp.model

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
    enum class Status(val order: Int) {
        Created(order = 1),
        Expired(order = 2),
        Paused(order = 3),
        WaitingForPayment(order = 4),
        PaymentProcessing(order = 5),
        Verifying(order = 6),
        Failed(order = 7),
        Paid(order = 8),
        Sending(order = 9),
        Finished(order = 10),
        ;

        val isTerminal: Boolean
            get() = when (this) {
                Expired,
                Failed,
                Paused,
                Finished,
                -> true
                Created,
                WaitingForPayment,
                PaymentProcessing,
                Verifying,
                Paid,
                Sending,
                -> false
            }

        val isHidden: Boolean
            get() = when (this) {
                Created,
                Expired,
                Paused,
                -> true
                WaitingForPayment,
                PaymentProcessing,
                Verifying,
                Failed,
                Paid,
                Sending,
                Finished,
                -> false
            }
    }
}