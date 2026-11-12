package com.tangem.data.tangempay.repository

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.data.tangempay.util.OrderRequestConverter
import com.tangem.data.tangempay.util.TangemPayOrderConverter
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.domain.tangempay.model.CustomerOrderRequest
import com.tangem.domain.tangempay.model.TangemPayOrder
import com.tangem.domain.tangempay.order.TangemPayOrderRepository
import com.tangem.spend.datasource.tangempay.TangemPayOrderApi
import javax.inject.Inject

internal class DefaultTangemPayOrderRepository @Inject constructor(
    private val orderApi: TangemPayOrderApi,
    private val requestHelper: TangemPayRequestPerformer,
) : TangemPayOrderRepository {

    override suspend fun getOrder(
        userWalletId: UserWalletId,
        orderId: String,
    ): Either<TangemPayApiError, TangemPayOrder> {
        return requestHelper.performRequest(userWalletId) { authHeader ->
            orderApi.getOrder(authHeader = authHeader, orderId = orderId)
        }.mapLeft { error ->
            if (error is TangemPayApiError.NotFound) TangemPayApiError.OrderNotFound else error
        }.map { response ->
            TangemPayOrderConverter.convert(response.result)
        }
    }

    override suspend fun findOrders(
        userWalletId: UserWalletId,
        types: Set<TangemPayOrder.Type>,
        statuses: Set<TangemPayOrder.Status>,
    ): Either<TangemPayApiError, List<TangemPayOrder>> {
        return requestHelper.performRequest(userWalletId) { authHeader ->
            orderApi.findOrders(
                authHeader = authHeader,
                orderTypes = types.map { it.toTypeId() },
                orderStatuses = statuses.mapNotNull { it.toStatusId() },
            )
        }.map { response ->
            response.result.map(TangemPayOrderConverter::convert)
        }
    }

    override suspend fun createOrder(
        userWalletId: UserWalletId,
        request: CustomerOrderRequest,
        idempotencyKey: String,
    ): Either<TangemPayApiError, TangemPayOrder> = either {
        val customerWalletAddress = catch({ requestHelper.getCustomerWalletAddress(userWalletId) }) { _ ->
            raise(TangemPayApiError.UnknownWithoutCode)
        }
        val body = OrderRequestConverter.convert(
            OrderRequestConverter.Input(customerWalletAddress = customerWalletAddress, request = request),
        ).bind()
        val response = requestHelper.performRequest(userWalletId) { authHeader ->
            orderApi.createOrder(authHeader = authHeader, idempotencyKey = idempotencyKey, body = body)
        }.bind()
        TangemPayOrderConverter.convert(response.result)
    }
}