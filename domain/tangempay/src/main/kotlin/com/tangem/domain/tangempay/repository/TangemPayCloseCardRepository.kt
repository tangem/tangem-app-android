package com.tangem.domain.tangempay.repository

import arrow.core.Either
import com.tangem.core.error.UniversalError
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.TangemPayOrderInfo
import com.tangem.domain.tangempay.error.TangemPayApiError

interface TangemPayCloseCardRepository {

    suspend fun closeCard(userWalletId: UserWalletId, cardId: String): Either<TangemPayApiError, TangemPayOrderInfo>

    suspend fun storeCloseOrderId(cardId: String, orderId: String): Either<UniversalError, Unit>

    suspend fun removeCloseOrderId(cardId: String): Either<UniversalError, Unit>

    suspend fun getCloseOrderId(userWalletId: UserWalletId, cardId: String): Either<UniversalError, String?>
}