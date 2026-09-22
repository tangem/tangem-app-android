package com.tangem.feature.tokendetails.store

import com.tangem.core.analytics.models.ExpressAnalyticsStatus

/**
 * Runtime cache for storing swap transaction statuses sent to analytics
 */
internal interface SwapTransactionStatusStore {
    suspend fun getTransactionStatus(txId: String): ExpressAnalyticsStatus?

    suspend fun setTransactionStatus(txId: String, status: ExpressAnalyticsStatus)
}