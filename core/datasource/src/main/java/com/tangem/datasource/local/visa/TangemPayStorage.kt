package com.tangem.datasource.local.visa

interface TangemPayStorage {

    suspend fun store(authHeader: String)

    suspend fun get(): String?

    suspend fun clear()
}