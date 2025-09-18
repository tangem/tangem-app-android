package com.tangem.datasource.local.token

interface YieldSupplyWarningActionStore {

    suspend fun getSync(): Set<String>

    suspend fun store(symbol: String)
}