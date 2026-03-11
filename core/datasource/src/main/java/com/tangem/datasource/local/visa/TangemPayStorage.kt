package com.tangem.datasource.local.visa

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.TangemPayWithdrawState
import com.tangem.domain.visa.model.TangemPayAuthTokens

@Suppress("TooManyFunctions")
interface TangemPayStorage {

    suspend fun storeCustomerWalletAddress(userWalletId: UserWalletId, customerWalletAddress: String)
    suspend fun getCustomerWalletAddress(userWalletId: UserWalletId): String?

    suspend fun clearCustomerWalletAddress(userWalletId: UserWalletId)

    suspend fun storeAuthTokens(customerWalletAddress: String, tokens: TangemPayAuthTokens)

    suspend fun getAuthTokens(customerWalletAddress: String): TangemPayAuthTokens?
    suspend fun clearAuthTokens(customerWalletAddress: String)

    suspend fun storeOrderId(customerWalletAddress: String, orderId: String)

    suspend fun getOrderId(customerWalletAddress: String): String?

    suspend fun clearOrderId(customerWalletAddress: String)

    suspend fun getAddToWalletDone(customerWalletAddress: String): Boolean

    suspend fun storeAddToWalletDone(customerWalletAddress: String, isDone: Boolean)
    suspend fun storeCheckCustomerWalletResult(userWalletId: UserWalletId, isPaeraCustomer: Boolean)
    suspend fun checkCustomerWalletResult(userWalletId: UserWalletId): Boolean?

    /** Called after creating withdraw order, active order id */
    suspend fun storeActiveWithdrawOrderId(userWalletId: UserWalletId, orderId: String)

    /** Called after creating withdraw order, saves order data */
    suspend fun storeWithdrawOrder(userWalletId: UserWalletId, data: TangemPayWithdrawState)

    /** Returns single active order id. Once the order is completed, deletes id from storage.
     * Only one active order allowed for a wallet */
    suspend fun getActiveWithdrawOrderId(userWalletId: UserWalletId): String?

    /** Returns all withdraw orders saved.
     * Once we get tx hash for an order, it gets deleted from this storage */
    suspend fun getWithdrawOrders(userWalletId: UserWalletId): List<TangemPayWithdrawState>?

    /** Deletes active withdraw order. Called after order is completed */
    suspend fun deleteActiveWithdrawOrder(userWalletId: UserWalletId)

    /** Deletes withdraw order data. Called after getting its tx hash */
    suspend fun deleteWithdrawOrder(userWalletId: UserWalletId, orderId: String)

    suspend fun getHideMainOnboardingBanner(userWalletId: UserWalletId): Boolean

    suspend fun storeHideOnboardingBanner(userWalletId: UserWalletId, hide: Boolean)
    suspend fun storeTangemPayEligibility(eligibility: Boolean)
    suspend fun getTangemPayEligibility(): Boolean

    suspend fun clearAll(userWalletId: UserWalletId, customerWalletAddress: String)
}