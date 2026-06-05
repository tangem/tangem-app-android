package com.tangem.data.pay.repository

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.data.pay.util.OrderStatusConverter
import com.tangem.datasource.api.pay.TangemPayApi
import com.tangem.datasource.api.pay.models.request.ReissueCardRequest
import com.tangem.datasource.local.visa.TangemPayReissueCardStore
import com.tangem.domain.models.pay.TangemPayReissueCardFee
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.TangemPayOrderInfo
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
    ): Either<VisaApiError, TangemPayOrderInfo> = either {
        val response = requestHelper.performRequest(userWalletId) { authHeader ->
            tangemPayApi.reissueCard(
                authHeader = authHeader,
                body = ReissueCardRequest(cardId = cardId),
            )
        }.bind()
        TangemPayOrderInfo(
            orderId = response.result.orderId,
            orderStatus = OrderStatusConverter.convert(response.result.status),
        )
    }

    private companion object {
        const val CARD_REPLACEMENT_FEE_TYPE = "CARD_REPLACEMENT"
    }
}