package com.tangem.data.pay.repository

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.right
import com.tangem.core.error.UniversalError
import com.tangem.datasource.api.pay.TangemPayApi
import com.tangem.datasource.api.pay.models.request.ReissueCardRequest
import com.tangem.datasource.api.pay.models.response.OrderResponse
import com.tangem.datasource.local.visa.TangemPayReissueCardStore
import com.tangem.domain.models.TangemPayReissueCardFee
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.TangemPayReissueOrderInfo
import com.tangem.domain.pay.repository.TangemPayReissueCardRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.utils.coroutines.runSuspendCatching
import javax.inject.Inject

internal class DefaultReissueCardRepository @Inject constructor(
    private val tangemPayApi: TangemPayApi,
    private val requestHelper: TangemPayRequestPerformer,
    private val tangemPayReissueCardStore: TangemPayReissueCardStore,
) : TangemPayReissueCardRepository {

    override suspend fun getReissueCardFee(userWalletId: UserWalletId): Either<VisaApiError, TangemPayReissueCardFee> =
        either {
            runSuspendCatching {
                tangemPayReissueCardStore.getReissueFee(userWalletId)?.let { return Either.Right(it) }
            }

            val response = requestHelper.performRequest(userWalletId) { authHeader ->
                tangemPayApi.getFee(
                    authHeader = authHeader,
                    type = CARD_REPLACEMENT_FEE_TYPE,
                )
            }.bind()

            val result = response.result
            val fee = TangemPayReissueCardFee(
                amount = result.amount.toBigDecimal(),
                currencyCode = result.currency,
            )

            runSuspendCatching {
                tangemPayReissueCardStore.storeReissueFee(userWalletId, fee)
            }

            fee
        }

    override suspend fun reissueCard(
        userWalletId: UserWalletId,
        cardId: String,
    ): Either<VisaApiError, TangemPayReissueOrderInfo> = either {
        val response = requestHelper.performRequest(userWalletId) { authHeader ->
            tangemPayApi.reissueCard(
                authHeader = authHeader,
                body = ReissueCardRequest(cardId = cardId),
            )
        }.bind()

        TangemPayReissueOrderInfo(response.result.orderId, OrderStatus.fromString(response.result.status))
    }

    override suspend fun storeReissueOrderId(cardId: String, orderId: String): Either<UniversalError, Unit> =
        runSuspendCatching {
            tangemPayReissueCardStore.storeReissueOrderId(cardId, orderId)
        }.fold(
            onSuccess = { Unit.right() },
            onFailure = { Either.Left(VisaApiError.Unspecified) },
        )

    override suspend fun getReissueOrderInfo(
        userWalletId: UserWalletId,
        cardId: String,
    ): Either<UniversalError, TangemPayReissueOrderInfo?> = either {
        val orderId = runSuspendCatching { tangemPayReissueCardStore.getOrderId(cardId) }.getOrNull()

        if (orderId == null) {
            return null.right()
        }

        val order = requestHelper.performRequest(userWalletId) { authHeader ->
            tangemPayApi.getOrder(authHeader, orderId)
        }.bind()

        val result = order.result ?: raise(VisaApiError.Unspecified)

        TangemPayReissueOrderInfo(
            orderId = result.id,
            orderStatus = when (result.status) {
                OrderResponse.Result.Status.NEW -> OrderStatus.NEW
                OrderResponse.Result.Status.PROCESSING -> OrderStatus.PROCESSING
                OrderResponse.Result.Status.COMPLETED -> OrderStatus.COMPLETED
                OrderResponse.Result.Status.CANCELED -> OrderStatus.CANCELED
            },
        )
    }

    private companion object {
        const val CARD_REPLACEMENT_FEE_TYPE = "CARD_REPLACEMENT"
    }
}