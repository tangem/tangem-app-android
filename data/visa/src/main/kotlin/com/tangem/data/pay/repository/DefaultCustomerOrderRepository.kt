package com.tangem.data.pay.repository

import arrow.core.Either
import com.tangem.datasource.api.pay.TangemPayApi
import com.tangem.datasource.api.pay.models.response.OrderResponse
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.OrderData
import com.tangem.domain.pay.model.OrderStatus
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
            val status = when (response.result?.status) {
                null -> OrderStatus.PROCESSING
                OrderResponse.Result.Status.NEW -> OrderStatus.NEW
                OrderResponse.Result.Status.PROCESSING -> OrderStatus.PROCESSING
                OrderResponse.Result.Status.COMPLETED -> OrderStatus.COMPLETED
                OrderResponse.Result.Status.CANCELED -> OrderStatus.CANCELED
            }
            OrderData(
                customerId = response.result?.customerId.orEmpty(),
                status = status,
                withdrawTxHash = response.result?.data?.transactionHash?.ifEmpty { null },
            )
        }
    }
}