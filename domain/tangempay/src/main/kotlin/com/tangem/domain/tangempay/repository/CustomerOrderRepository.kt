package com.tangem.domain.tangempay.repository

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.CustomerOrderRequest
import com.tangem.domain.tangempay.model.Order
import com.tangem.domain.tangempay.model.OrderData
import com.tangem.domain.tangempay.model.OrderStatus
import com.tangem.domain.tangempay.model.OrderType
import com.tangem.domain.tangempay.error.TangemPayApiError

interface CustomerOrderRepository {

    suspend fun getOrderData(userWalletId: UserWalletId, orderId: String): Either<TangemPayApiError, OrderData>

    /**
     * Find orders matching the given filters.
     *
     * This is the source of truth for resolving active orders — a locally stored `orderId` is only a hint.
     *
     * @param types order types to include; pass an empty set for "any type".
     * @param statuses statuses to include; pass an empty set for "any status". Use `{NEW, PROCESSING}`
     *        (i.e. [OrderStatus.isActive]) for active-only queries.
     */
    suspend fun findOrders(
        userWalletId: UserWalletId,
        types: Set<OrderType> = emptySet(),
        statuses: Set<OrderStatus> = emptySet(),
    ): Either<TangemPayApiError, List<Order>>

    /**
     * Create a new order via `POST /v1/order` with a per-attempt idempotency key.
     * The caller is responsible for finding an existing active order before creating a new one.
     */
    suspend fun createOrder(
        userWalletId: UserWalletId,
        request: CustomerOrderRequest,
        idempotencyKey: String,
    ): Either<TangemPayApiError, Order>

    suspend fun cancelOrder(userWalletId: UserWalletId, orderId: String): Either<TangemPayApiError, Unit>
}