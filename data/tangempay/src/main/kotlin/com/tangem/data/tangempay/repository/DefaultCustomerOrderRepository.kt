package com.tangem.data.tangempay.repository

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.tangem.data.tangempay.util.OrderConverter
import com.tangem.data.tangempay.util.OrderRequestConverter
import com.tangem.data.tangempay.util.OrderStatusConverter
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.CustomerOrderRequest
import com.tangem.domain.tangempay.model.Order
import com.tangem.domain.tangempay.model.OrderData
import com.tangem.domain.tangempay.model.OrderStatus
import com.tangem.domain.tangempay.model.OrderType
import com.tangem.domain.tangempay.repository.CustomerOrderRepository
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.spend.datasource.tangempay.TangemPayApi
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject

internal class DefaultCustomerOrderRepository @Inject constructor(
    private val tangemPayApi: TangemPayApi,
    private val requestHelper: TangemPayRequestPerformer,
) : CustomerOrderRepository {

    override suspend fun getOrderData(
        userWalletId: UserWalletId,
        orderId: String,
    ): Either<TangemPayApiError, OrderData> {
        return requestHelper.performRequest(userWalletId) { authHeader ->
            tangemPayApi.getOrder(authHeader = authHeader, orderId = orderId)
        }.mapLeft { error ->
            if (error is TangemPayApiError.NotFound) TangemPayApiError.OrderNotFound else error
        }.map { response ->
            val status = response.result?.status?.let(OrderStatusConverter::convert) ?: OrderStatus.PROCESSING
            OrderData(
                customerId = response.result?.customerId.orEmpty(),
                status = status,
                withdrawTxHash = response.result?.data?.transactionHash?.ifEmpty { null },
            )
        }
    }

    override suspend fun findOrders(
        userWalletId: UserWalletId,
        types: Set<OrderType>,
        statuses: Set<OrderStatus>,
    ): Either<TangemPayApiError, List<Order>> {
        val typeWire = types.map(OrderType::wireValue).takeIf { it.isNotEmpty() }
        val statusWire = statuses.map(OrderStatus::name).takeIf { it.isNotEmpty() }
        return requestHelper.performRequest(userWalletId) { authHeader ->
            tangemPayApi.findOrders(
                authHeader = authHeader,
                orderTypes = typeWire,
                orderStatuses = statusWire,
            )
        }.map { response ->
            response.result.map(OrderConverter::convert)
        }
    }

    override suspend fun createOrder(
        userWalletId: UserWalletId,
        request: CustomerOrderRequest,
        idempotencyKey: String,
    ): Either<TangemPayApiError, Order> = either {
        val customerWalletAddress = catch({ requestHelper.getCustomerWalletAddress(userWalletId) }) { throwable ->
            TangemLogger.e("Customer wallet address is not available for $userWalletId", throwable)
            raise(TangemPayApiError.UnknownWithoutCode)
        }
        val body = OrderRequestConverter.convert(
            OrderRequestConverter.Input(customerWalletAddress = customerWalletAddress, request = request),
        ).bind()
        val response = requestHelper.performRequest(userWalletId) { authHeader ->
            tangemPayApi.createOrder(authHeader = authHeader, idempotencyKey = idempotencyKey, body = body)
        }.bind()
        val result = ensureNotNull(response.result) { TangemPayApiError.UnknownWithoutCode }
        OrderConverter.convert(result)
    }

    override suspend fun cancelOrder(userWalletId: UserWalletId, orderId: String): Either<TangemPayApiError, Unit> {
        return requestHelper.performRequest(userWalletId) { authHeader ->
            tangemPayApi.cancelOrder(authHeader = authHeader, orderId = orderId)
        }.map {}
    }
}