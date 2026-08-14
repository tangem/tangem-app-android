package com.tangem.data.pay.repository

import arrow.core.Either
import com.tangem.data.pay.util.CardDeliveryQuoteConverter
import com.tangem.spend.datasource.pay.TangemPayApi
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CardDeliveryContext
import com.tangem.domain.pay.model.CardDeliveryQuote
import com.tangem.domain.pay.repository.CardDeliveryQuoteRepository
import com.tangem.domain.visa.error.VisaApiError
import javax.inject.Inject

internal class DefaultCardDeliveryQuoteRepository @Inject constructor(
    private val tangemPayApi: TangemPayApi,
    private val requestHelper: TangemPayRequestPerformer,
) : CardDeliveryQuoteRepository {

    override suspend fun getCardDeliveryQuote(
        userWalletId: UserWalletId,
        context: CardDeliveryContext,
    ): Either<VisaApiError, CardDeliveryQuote> {
        return requestHelper.performRequest(userWalletId) { authHeader ->
            tangemPayApi.getCardDeliveryQuote(authHeader = authHeader, context = context.queryValue)
        }.map(CardDeliveryQuoteConverter::convert)
    }
}