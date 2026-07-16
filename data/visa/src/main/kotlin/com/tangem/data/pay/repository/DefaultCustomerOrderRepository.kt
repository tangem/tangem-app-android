package com.tangem.data.pay.repository

import arrow.core.Either
import com.tangem.data.pay.util.OrderConverter
import com.tangem.data.pay.util.OrderStatusConverter
import com.tangem.datasource.api.pay.TangemPayApi
import com.tangem.datasource.api.pay.models.request.OrderRequest
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.Order
import com.tangem.domain.pay.model.OrderData
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.pay.repository.CustomerOrderRepository
import com.tangem.domain.visa.error.VisaApiError
import javax.inject.Inject

internal class DefaultCustomerOrderRepository @Inject constructor(
    private val tangemPayApi: TangemPayApi,
    private val requestHelper: TangemPayRequestPerformer,
) : CustomerOrderRepository {

    override suspend fun getOrderData(userWalletId: UserWalletId, orderId: String): Either<VisaApiError, OrderData> {
        return requestHelper.performRequest(userWalletId) { authHeader ->
            tangemPayApi.getOrder(authHeader = authHeader, orderId = orderId)
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
        type: OrderType,
        specificationName: String,
        idempotencyKey: String,
    ): Either<VisaApiError, Order> {
        val walletAddress = requestHelper.getCustomerWalletAddress(userWalletId)
        return requestHelper.performRequest(userWalletId) { authHeader ->
            tangemPayApi.createOrder(
                authHeader = authHeader,
                body = OrderRequest(
                    data = OrderRequest.Data(
                        customerWalletAddress = walletAddress,
                        specificationName = specificationName,
                        type = type.wireValue,
                    ),
                    idempotencyKey = idempotencyKey,
                ),
            )
        }.map { response ->
            val result = requireNotNull(response.result) { "createOrder returned empty result" }
            OrderConverter.convert(result)
        }
    }
}