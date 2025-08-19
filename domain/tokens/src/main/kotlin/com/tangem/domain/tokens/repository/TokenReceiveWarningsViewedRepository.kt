package com.tangem.domain.tokens.repository

interface TokenReceiveWarningsViewedRepository {

    suspend fun getViewedWarnings(): Set<String>

    suspend fun view(symbol: String)
}