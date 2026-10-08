package com.tangem.data.pay.repository

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.tangem.data.pay.util.OrderConverter
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.repository.MoneyAccountOrderRepository
import com.tangem.domain.pay.model.Order
import com.tangem.domain.pay.model.TangemPayOrderInfo
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.spend.datasource.pay.TangemPayApi
import com.tangem.spend.datasource.pay.models.request.OrderRequest
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

internal class DefaultMoneyAccountOrderRepository @Inject constructor(
    private val tangemPayApi: TangemPayApi,
    private val requestHelper: TangemPayRequestPerformer,
    private val dispatcherProvider: CoroutineDispatcherProvider,
) : MoneyAccountOrderRepository {

    private val orderStates = ConcurrentHashMap<UserWalletId, TangemPayOrderInfo>()

    override suspend fun createOrder(
        userWalletId: UserWalletId,
        depositAddress: String,
        signature: String,
    ): Either<VisaApiError, Order> = withContext(dispatcherProvider.io) {
        requestHelper.performRequest(userWalletId) { authHeader ->
            tangemPayApi.createOrder(
                authHeader = authHeader,
                idempotencyKey = UUID.randomUUID().toString(),
                body = OrderRequest(
                    data = OrderRequest.BankingAccountIssue(depositAddress = depositAddress, signature = signature),
                ),
            )
        }.flatMap { response ->
            val result = response.result ?: return@flatMap VisaApiError.UnknownWithoutCode.left()
            OrderConverter.convert(result).right()
        }
    }

    override fun storeOrder(userWalletId: UserWalletId, order: TangemPayOrderInfo) {
        orderStates[userWalletId] = order
    }

    override fun getOrder(userWalletId: UserWalletId): TangemPayOrderInfo? = orderStates[userWalletId]

    override fun clearOrder(userWalletId: UserWalletId) {
        orderStates.remove(userWalletId)
    }
}