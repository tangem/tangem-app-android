package com.tangem.datasource.local.visa

import com.tangem.domain.visa.model.VisaAuthTokens

interface TangemPayStorage {

    suspend fun storeAuthTokens(customerWalletAddress: String, tokens: VisaAuthTokens)

    suspend fun getAuthTokens(customerWalletAddress: String): VisaAuthTokens?

    suspend fun storeCustomerWalletAddress(customerWalletAddress: String)

    suspend fun getCustomerWalletAddress(): String?

    suspend fun clear(customerWalletAddress: String)
}