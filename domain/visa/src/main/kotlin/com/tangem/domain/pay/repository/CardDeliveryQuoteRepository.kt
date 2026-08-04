package com.tangem.domain.pay.repository

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CardDeliveryContext
import com.tangem.domain.pay.model.CardDeliveryQuote
import com.tangem.domain.visa.error.VisaApiError

interface CardDeliveryQuoteRepository {

    suspend fun getCardDeliveryQuote(
        userWalletId: UserWalletId,
        context: CardDeliveryContext,
    ): Either<VisaApiError, CardDeliveryQuote>
}