package com.tangem.domain.tangempay.repository

import arrow.core.Either
import com.tangem.core.error.UniversalError
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.tangempay.TangemPayReissueCardFee
import com.tangem.domain.tangempay.model.TangemPayOrderInfo
import com.tangem.domain.tangempay.error.TangemPayApiError

interface TangemPayReissueCardRepository {

    suspend fun getReissueCardFee(userWalletId: UserWalletId): Either<TangemPayApiError, TangemPayReissueCardFee>

    suspend fun reissueCard(userWalletId: UserWalletId, cardId: String): Either<TangemPayApiError, TangemPayOrderInfo>

    suspend fun storeReissueOrderId(cardId: String, orderId: String): Either<UniversalError, Unit>

    suspend fun removeReissueOrderId(cardId: String): Either<UniversalError, Unit>

    suspend fun getReissueOrderId(userWalletId: UserWalletId, cardId: String): Either<UniversalError, String?>
}