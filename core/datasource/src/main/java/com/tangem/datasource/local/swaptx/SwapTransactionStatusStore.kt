package com.tangem.datasource.local.swaptx

/**
 * Runtime cache for storing swap transactions statuses sent to analytics
 */
interface SwapTransactionStatusStore {
    suspend fun getTransactionStatus(txId: String): ExchangeAnalyticsStatus?

    suspend fun setTransactionStatus(txId: String, status: ExchangeAnalyticsStatus)
}

enum class ExchangeAnalyticsStatus(val value: String) {
    WaitingTxHash("Waiting tx hash"),
    InProgress("In Progress"),
    Done("Done"),
    Fail("Fail"),
    FailTx("Fail tx"),
    Unknown("Unknown"),
    KYC("KYC"),
    Refunded("Refunded"),
    Cancelled("Canceled"),
}