package com.tangem.datasource.local.swap

import com.tangem.datasource.local.datastore.core.StringKeyDataStore

internal class DefaultSwapTransactionStatusStore(
    private val dataStore: StringKeyDataStore<ExpressAnalyticsStatus>,
) : SwapTransactionStatusStore, StringKeyDataStore<ExpressAnalyticsStatus> by dataStore {

    override suspend fun getTransactionStatus(txId: String) = getSyncOrNull(txId)

    override suspend fun setTransactionStatus(txId: String, status: ExpressAnalyticsStatus) = store(txId, status)
}