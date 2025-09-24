package com.tangem.domain.tokens.repository

interface YieldSupplyWarningsViewedRepository {

    suspend fun getViewedWarnings(): Set<String>

    suspend fun view(symbol: String)
}