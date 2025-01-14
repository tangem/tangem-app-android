package com.tangem.datasource.api.common.visa

interface TangemVisaAuthProvider {

    suspend fun getAuthHeader(): String
}