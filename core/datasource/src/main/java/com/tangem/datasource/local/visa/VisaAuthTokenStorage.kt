package com.tangem.datasource.local.visa

import com.tangem.domain.visa.model.VisaAuthTokens

interface VisaAuthTokenStorage {

    suspend fun store(tokens: VisaAuthTokens)

    suspend fun get(): VisaAuthTokens?
}