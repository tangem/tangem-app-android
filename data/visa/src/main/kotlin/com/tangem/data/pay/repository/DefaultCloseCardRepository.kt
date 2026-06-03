package com.tangem.data.pay.repository

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.right
import com.tangem.core.error.UniversalError
import com.tangem.data.pay.util.OrderStatusConverter
import com.tangem.datasource.api.pay.TangemPayApi
import com.tangem.datasource.api.pay.models.request.CloseCardRequest
import com.tangem.datasource.local.visa.TangemPayCloseCardStore
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.TangemPayOrderInfo
import com.tangem.domain.pay.repository.TangemPayCloseCardRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.utils.coroutines.runSuspendCatching
import javax.inject.Inject

internal class DefaultCloseCardRepository @Inject constructor(
    private val tangemPayApi: TangemPayApi,
    private val requestHelper: TangemPayRequestPerformer,
    private val tangemPayCloseCardStore: TangemPayCloseCardStore,
) : TangemPayCloseCardRepository {

    override suspend fun closeCard(
        userWalletId: UserWalletId,
        cardId: String,
    ): Either<VisaApiError, TangemPayOrderInfo> = either {
        val response = requestHelper.performRequest(userWalletId) { authHeader ->
            tangemPayApi.closeCard(
                authHeader = authHeader,
                body = CloseCardRequest(cardId = cardId),
            )
        }.bind()
        TangemPayOrderInfo(
            orderId = response.result.orderId,
            orderStatus = OrderStatusConverter.convert(response.result.status),
        )
    }

    override suspend fun setCloseOrderId(cardId: String, orderId: String?): Either<UniversalError, Unit> =
        runSuspendCatching {
            tangemPayCloseCardStore.setCloseOrderId(cardId, orderId)
        }.fold(
            onSuccess = { Unit.right() },
            onFailure = { Either.Left(VisaApiError.Unspecified) },
        )

    override suspend fun getCloseOrderId(userWalletId: UserWalletId, cardId: String): Either<UniversalError, String?> =
        either {
            runSuspendCatching { tangemPayCloseCardStore.getOrderId(cardId) }.getOrNull()
        }
}