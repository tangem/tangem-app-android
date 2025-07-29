package com.tangem.datasource.local.swap

/**
 * Runtime cache for storing swap transactions statuses sent to analytics
 */
interface SwapTransactionStatusStore {
    suspend fun getTransactionStatus(txId: String): ExpressAnalyticsStatus?

    suspend fun setTransactionStatus(txId: String, status: ExpressAnalyticsStatus)
}

enum class ExpressAnalyticsStatus(val value: String) {
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