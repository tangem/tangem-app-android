package com.tangem.domain.tangempay.order

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.domain.tangempay.model.CustomerOrderRequest
import com.tangem.domain.tangempay.model.TangemPayOrder

interface TangemPayOrderRepository {

    suspend fun getOrder(userWalletId: UserWalletId, orderId: String): Either<TangemPayApiError, TangemPayOrder>

    suspend fun findOrders(
        userWalletId: UserWalletId,
        types: Set<TangemPayOrder.Type>,
        statuses: Set<TangemPayOrder.Status>,
    ): Either<TangemPayApiError, List<TangemPayOrder>>

    suspend fun createOrder(
        userWalletId: UserWalletId,
        request: CustomerOrderRequest,
        idempotencyKey: String,
    ): Either<TangemPayApiError, TangemPayOrder>
}