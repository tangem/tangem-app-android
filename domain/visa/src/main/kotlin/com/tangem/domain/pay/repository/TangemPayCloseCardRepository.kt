package com.tangem.domain.pay.repository

import arrow.core.Either
import com.tangem.core.error.UniversalError
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.TangemPayOrderInfo
import com.tangem.domain.visa.error.VisaApiError

interface TangemPayCloseCardRepository {

    suspend fun closeCard(userWalletId: UserWalletId, cardId: String): Either<VisaApiError, TangemPayOrderInfo>

    suspend fun storeCloseOrderId(cardId: String, orderId: String): Either<UniversalError, Unit>

    suspend fun removeCloseOrderId(cardId: String): Either<UniversalError, Unit>

    suspend fun getCloseOrderId(userWalletId: UserWalletId, cardId: String): Either<UniversalError, String?>
}