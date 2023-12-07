package com.tangem.datasource.local.swaptx

import com.tangem.datasource.local.datastore.core.StringKeyDataStore

internal class DefaultSwapTransactionStatusStore(
    private val dataStore: StringKeyDataStore<ExchangeAnalyticsStatus>,
) : SwapTransactionStatusStore, StringKeyDataStore<ExchangeAnalyticsStatus> by dataStore {

    override suspend fun getTransactionStatus(txId: String) = getSyncOrNull(txId)

    override suspend fun setTransactionStatus(txId: String, status: ExchangeAnalyticsStatus) = store(txId, status)
}
