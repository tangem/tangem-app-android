package com.tangem.datasource.local.visa

interface PayStorage {

    suspend fun store(authHeader: String)

    suspend fun get(): String?

    suspend fun clear()
}