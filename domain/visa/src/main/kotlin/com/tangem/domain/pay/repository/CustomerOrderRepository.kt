package com.tangem.domain.pay.repository

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.visa.error.VisaApiError

interface CustomerOrderRepository {

    suspend fun getOrderStatus(userWalletId: UserWalletId, orderId: String): Either<VisaApiError, OrderStatus>

    suspend fun hasWithdrawOrder(userWalletId: UserWalletId): Boolean
}