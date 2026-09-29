package com.tangem.data.pay.repository

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.tangem.data.pay.util.OrderConverter
import com.tangem.data.pay.util.OrderRequestConverter
import com.tangem.data.pay.util.OrderStatusConverter
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CustomerOrderRequest
import com.tangem.domain.pay.model.Order
import com.tangem.domain.pay.model.OrderData
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.pay.repository.CustomerOrderRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.spend.datasource.pay.TangemPayApi
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject

internal class DefaultCustomerOrderRepository @Inject constructor(
    private val tangemPayApi: TangemPayApi,
    private val requestHelper: TangemPayRequestPerformer,
) : CustomerOrderRepository {

    override suspend fun getOrderData(userWalletId: UserWalletId, orderId: String): Either<VisaApiError, OrderData> {
        return requestHelper.performRequest(userWalletId) { authHeader ->
            tangemPayApi.getOrder(authHeader = authHeader, orderId = orderId)
        }.mapLeft { error ->
            if (error is VisaApiError.NotFound) VisaApiError.OrderNotFound else error
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
    ): Either<VisaApiError, List<Order>> {
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
    ): Either<VisaApiError, Order> = either {
        val customerWalletAddress = catch({ requestHelper.getCustomerWalletAddress(userWalletId) }) { throwable ->
            TangemLogger.e("Customer wallet address is not available for $userWalletId", throwable)
            raise(VisaApiError.UnknownWithoutCode)
        }
        val body = OrderRequestConverter.convert(
            OrderRequestConverter.Input(
                customerWalletAddress = customerWalletAddress,
                request = request,
                idempotencyKey = idempotencyKey,
            ),
        ).bind()
        val response = requestHelper.performRequest(userWalletId) { authHeader ->
            tangemPayApi.createOrder(authHeader = authHeader, body = body)
        }.bind()
        val result = ensureNotNull(response.result) { VisaApiError.UnknownWithoutCode }
        OrderConverter.convert(result)
    }

    override suspend fun cancelOrder(userWalletId: UserWalletId, orderId: String): Either<VisaApiError, Unit> {
        return requestHelper.performRequest(userWalletId) { authHeader ->
            tangemPayApi.cancelOrder(authHeader = authHeader, orderId = orderId)
        }.map {}
    }
}