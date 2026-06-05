package com.tangem.data.pay.repository

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.data.pay.util.OrderStatusConverter
import com.tangem.datasource.api.pay.TangemPayApi
import com.tangem.datasource.api.pay.models.request.CloseCardRequest
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.TangemPayOrderInfo
import com.tangem.domain.pay.repository.TangemPayCloseCardRepository
import com.tangem.domain.visa.error.VisaApiError
import javax.inject.Inject

internal class DefaultCloseCardRepository @Inject constructor(
    private val tangemPayApi: TangemPayApi,
    private val requestHelper: TangemPayRequestPerformer,
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
}