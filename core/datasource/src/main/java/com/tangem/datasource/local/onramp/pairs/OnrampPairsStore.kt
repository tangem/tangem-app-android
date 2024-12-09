package com.tangem.datasource.local.onramp.pairs

import com.tangem.domain.onramp.model.OnrampPair

interface OnrampPairsStore {
    suspend fun getSyncOrNull(key: String): List<OnrampPair>?
    suspend fun store(key: String, value: List<OnrampPair>)
    suspend fun clear()
}