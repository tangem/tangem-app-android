package com.tangem.datasource.local.visa

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.visa.model.VisaAuthTokens

interface TangemPayStorage {

    suspend fun storeCustomerWalletAddress(userWalletId: UserWalletId, customerWalletAddress: String)
    suspend fun getCustomerWalletAddress(userWalletId: UserWalletId): String?

    suspend fun storeAuthTokens(customerWalletAddress: String, tokens: VisaAuthTokens)

    suspend fun getAuthTokens(customerWalletAddress: String): VisaAuthTokens?

    suspend fun storeOrderId(customerWalletAddress: String, orderId: String)

    suspend fun getOrderId(customerWalletAddress: String): String?

    suspend fun clearOrderId(customerWalletAddress: String)

    suspend fun getAddToWalletDone(customerWalletAddress: String): Boolean

    suspend fun storeAddToWalletDone(customerWalletAddress: String, isDone: Boolean)

    suspend fun clearAll(userWalletId: UserWalletId, customerWalletAddress: String)
}