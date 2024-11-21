package com.tangem.datasource.local.onramp.pairs

import com.tangem.datasource.api.onramp.models.response.model.OnrampPairDTO

interface OnrampPairsStore {
    suspend fun getSyncOrNull(key: String): List<OnrampPairDTO>?
    suspend fun store(key: String, value: List<OnrampPairDTO>)
    suspend fun clear()
}
