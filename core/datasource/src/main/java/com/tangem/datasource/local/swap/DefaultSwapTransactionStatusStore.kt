package com.tangem.datasource.local.swap

import com.tangem.core.local.datastore.RuntimeSharedMapStore

internal class DefaultSwapTransactionStatusStore(
    private val store: RuntimeSharedMapStore<String, ExpressAnalyticsStatus>,
) : SwapTransactionStatusStore {

    override suspend fun getTransactionStatus(txId: String): ExpressAnalyticsStatus? = store.getSyncOrNull(txId)

    override suspend fun setTransactionStatus(txId: String, status: ExpressAnalyticsStatus) = store.store(txId, status)
}