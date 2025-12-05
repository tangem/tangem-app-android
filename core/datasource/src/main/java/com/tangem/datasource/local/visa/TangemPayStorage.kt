package com.tangem.datasource.local.visa

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.visa.model.TangemPayAuthTokens

interface TangemPayStorage {

    suspend fun storeCustomerWalletAddress(userWalletId: UserWalletId, customerWalletAddress: String)
    suspend fun getCustomerWalletAddress(userWalletId: UserWalletId): String?

    suspend fun storeAuthTokens(customerWalletAddress: String, tokens: TangemPayAuthTokens)

    suspend fun getAuthTokens(customerWalletAddress: String): TangemPayAuthTokens?

    suspend fun storeOrderId(customerWalletAddress: String, orderId: String)

    suspend fun getOrderId(customerWalletAddress: String): String?

    suspend fun clearOrderId(customerWalletAddress: String)

    suspend fun getAddToWalletDone(customerWalletAddress: String): Boolean

    suspend fun storeAddToWalletDone(customerWalletAddress: String, isDone: Boolean)
    suspend fun storeCheckCustomerWalletResult(userWalletId: UserWalletId, isPaeraCustomer: Boolean)
    suspend fun checkCustomerWalletResult(userWalletId: UserWalletId): Boolean?

    suspend fun storeWithdrawOrder(userWalletId: UserWalletId, orderId: String)

    suspend fun getWithdrawOrderId(userWalletId: UserWalletId): String?

    suspend fun deleteWithdrawOrder(userWalletId: UserWalletId)

    suspend fun clearAll(userWalletId: UserWalletId, customerWalletAddress: String)
}