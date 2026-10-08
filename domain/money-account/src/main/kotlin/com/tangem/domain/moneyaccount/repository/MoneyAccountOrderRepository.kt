package com.tangem.domain.moneyaccount.repository

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.Order
import com.tangem.domain.pay.model.TangemPayOrderInfo
import com.tangem.domain.visa.error.VisaApiError

interface MoneyAccountOrderRepository {

    suspend fun createOrder(
        userWalletId: UserWalletId,
        depositAddress: String,
        signature: String,
    ): Either<VisaApiError, Order>

    fun storeOrder(userWalletId: UserWalletId, order: TangemPayOrderInfo)

    fun getOrder(userWalletId: UserWalletId): TangemPayOrderInfo?

    fun clearOrder(userWalletId: UserWalletId)
}