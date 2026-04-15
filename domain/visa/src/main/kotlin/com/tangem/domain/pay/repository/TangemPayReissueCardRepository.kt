package com.tangem.domain.pay.repository

import arrow.core.Either
import com.tangem.core.error.UniversalError
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.TangemPayReissueCardFee
import com.tangem.domain.pay.model.TangemPayReissueOrderInfo
import com.tangem.domain.visa.error.VisaApiError

interface TangemPayReissueCardRepository {

    suspend fun getReissueCardFee(userWalletId: UserWalletId): Either<VisaApiError, TangemPayReissueCardFee>

    suspend fun reissueCard(userWalletId: UserWalletId, cardId: String): Either<VisaApiError, TangemPayReissueOrderInfo>

    suspend fun storeReissueOrderId(cardId: String, orderId: String): Either<UniversalError, Unit>

    suspend fun getReissueOrderInfo(
        userWalletId: UserWalletId,
        cardId: String,
    ): Either<UniversalError, TangemPayReissueOrderInfo?>
}