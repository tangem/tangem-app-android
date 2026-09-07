package com.tangem.data.tokens.store

internal interface TokenReceiveWarningActionStore {

    suspend fun getSync(): Set<String>

    suspend fun store(symbol: String)
}