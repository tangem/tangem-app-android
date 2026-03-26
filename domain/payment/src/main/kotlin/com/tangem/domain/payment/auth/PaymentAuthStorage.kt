package com.tangem.domain.payment.auth

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.payment.models.auth.PaymentAuthTokens

interface PaymentAuthStorage {

    suspend fun storeCustomerWalletAddress(userWalletId: UserWalletId, customerWalletAddress: String)

    suspend fun getCustomerWalletAddress(userWalletId: UserWalletId): String?

    suspend fun storeAuthTokens(customerWalletAddress: String, tokens: PaymentAuthTokens)

    suspend fun getAuthTokens(customerWalletAddress: String): PaymentAuthTokens?
}