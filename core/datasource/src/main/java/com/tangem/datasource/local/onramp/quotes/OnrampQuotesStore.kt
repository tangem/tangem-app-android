package com.tangem.datasource.local.onramp.quotes

import com.tangem.datasource.api.onramp.models.response.OnrampQuoteResponse

interface OnrampQuotesStore {
    suspend fun getSyncOrNull(key: String): List<OnrampQuoteResponse>?
    suspend fun store(key: String, value: List<OnrampQuoteResponse>)
    suspend fun clear()
}
