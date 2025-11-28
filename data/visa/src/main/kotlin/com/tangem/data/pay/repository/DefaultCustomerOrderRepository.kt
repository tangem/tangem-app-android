package com.tangem.data.pay.repository

import arrow.core.Either
import com.tangem.datasource.api.pay.TangemPayApi
import com.tangem.datasource.local.visa.TangemPayStorage
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.repository.CustomerOrderRepository
import com.tangem.domain.visa.error.VisaApiError
import javax.inject.Inject

internal class DefaultCustomerOrderRepository @Inject constructor(
    private val tangemPayApi: TangemPayApi,
    private val requestHelper: TangemPayRequestPerformer,
    private val tangemPayStorage: TangemPayStorage,
) : CustomerOrderRepository {

    override suspend fun getOrderStatus(
        userWalletId: UserWalletId,
        orderId: String,
    ): Either<VisaApiError, OrderStatus> {
        return requestHelper.performRequest(userWalletId) { authHeader ->
            tangemPayApi.getOrder(authHeader = authHeader, orderId = orderId)
        }.map { response ->
            when (response.result?.status) {
                null -> OrderStatus.UNKNOWN
                OrderStatus.NEW.apiName -> OrderStatus.NEW
                OrderStatus.PROCESSING.apiName -> OrderStatus.PROCESSING
                OrderStatus.COMPLETED.apiName -> OrderStatus.COMPLETED
                else -> OrderStatus.CANCELED
            }
        }
    }

    override suspend fun hasWithdrawOrder(userWalletId: UserWalletId): Boolean {
        val orderId = tangemPayStorage.getWithdrawOrderId(userWalletId)
        if (orderId == null) return false

        val status = getOrderStatus(userWalletId, orderId).getOrNull()

        val hasActiveOrder = status == OrderStatus.NEW || status == OrderStatus.PROCESSING
        if (!hasActiveOrder) tangemPayStorage.deleteWithdrawOrder(userWalletId)

        return hasActiveOrder
    }
}