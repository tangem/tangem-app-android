package com.tangem.data.pay.repository

import arrow.core.Either
import com.tangem.core.error.UniversalError
import com.tangem.datasource.api.pay.TangemPayApi
import com.tangem.domain.pay.model.TangemPayCardBalance
import com.tangem.domain.pay.model.TangemPayCardDetails
import com.tangem.domain.pay.repository.CardDetailsRepository
import javax.inject.Inject

private const val TAG = "TangemPay: CardDetailsRepository"

internal class DefaultCardDetailsRepository @Inject constructor(
    private val tangemPayApi: TangemPayApi,
    private val requestHelper: TangemPayRequestPerformer,
) : CardDetailsRepository {

    override suspend fun getCardBalance(): Either<UniversalError, TangemPayCardBalance> {
        return requestHelper.runWithErrorLogs(TAG) {
            val result = requestHelper.request { authHeader ->
                tangemPayApi.getCardBalance(authHeader)
            }.result ?: error("Cannot get card balance")
            TangemPayCardBalance(
                balance = result.availableBalance,
                currencyCode = result.currency,
            )
        }
    }

    override suspend fun revealCardDetails(): Either<UniversalError, TangemPayCardDetails> {
        TODO("[REDACTED_TASK_KEY] add reveal card details")
    }
}