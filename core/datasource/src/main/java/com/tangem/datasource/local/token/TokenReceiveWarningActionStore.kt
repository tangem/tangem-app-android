package com.tangem.datasource.local.token

interface TokenReceiveWarningActionStore {

    suspend fun getSync(): Set<String>

    suspend fun store(symbol: String)
}