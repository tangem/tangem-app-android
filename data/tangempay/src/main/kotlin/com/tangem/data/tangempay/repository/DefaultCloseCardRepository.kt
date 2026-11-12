package com.tangem.data.tangempay.repository

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.right
import com.tangem.core.error.UniversalError
import com.tangem.data.tangempay.util.OrderStatusConverter
import com.tangem.spend.datasource.tangempay.TangemPayApi
import com.tangem.spend.datasource.tangempay.models.request.CloseCardRequest
import com.tangem.spend.datasource.tangempay.store.TangemPayCloseCardStore
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.TangemPayOrderInfo
import com.tangem.domain.tangempay.repository.TangemPayCloseCardRepository
import com.tangem.domain.tangempay.error.TangemPayApiError
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
    ): Either<TangemPayApiError, TangemPayOrderInfo> = either {
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

    override suspend fun storeCloseOrderId(cardId: String, orderId: String): Either<UniversalError, Unit> =
        runSuspendCatching {
            tangemPayCloseCardStore.storeCloseOrderId(cardId, orderId)
        }.fold(
            onSuccess = { Unit.right() },
            onFailure = { Either.Left(TangemPayApiError.Unspecified) },
        )

    override suspend fun removeCloseOrderId(cardId: String): Either<UniversalError, Unit> = runSuspendCatching {
        tangemPayCloseCardStore.removeCloseOrderId(cardId)
    }.fold(
        onSuccess = { Unit.right() },
        onFailure = { Either.Left(TangemPayApiError.Unspecified) },
    )

    override suspend fun getCloseOrderId(userWalletId: UserWalletId, cardId: String): Either<UniversalError, String?> =
        either {
            runSuspendCatching { tangemPayCloseCardStore.getOrderId(cardId) }.getOrNull()
        }
}