package com.tangem.domain.pay.repository

import arrow.core.Either
import com.tangem.domain.models.account.TangemPayTariffPlanTransition
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.Order
import com.tangem.domain.pay.model.OrderData
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.visa.error.VisaApiError

interface CustomerOrderRepository {

    suspend fun getOrderData(userWalletId: UserWalletId, orderId: String): Either<VisaApiError, OrderData>

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
    ): Either<VisaApiError, List<Order>>

    /**
     * Create a new order via `POST /v1/order` with a per-attempt idempotency key.
     * The caller is responsible for finding an existing active order before creating a new one.
     *
     * @param chainId EVM chain id for [OrderType.SMART_CONTRACT_ISSUE_RAIN] (network contract-creation)
     * orders; `null` for every other order type.
     */
    suspend fun createOrder(
        userWalletId: UserWalletId,
        type: OrderType,
        specificationName: String?,
        idempotencyKey: String,
        targetTariffPlanId: String? = null,
        transitionType: TangemPayTariffPlanTransition.Type? = null,
        chainId: Int? = null,
    ): Either<VisaApiError, Order>

    suspend fun cancelOrder(userWalletId: UserWalletId, orderId: String): Either<VisaApiError, Unit>
}