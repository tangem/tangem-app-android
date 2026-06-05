package com.tangem.domain.pay.repository

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.pay.TangemPayReissueCardFee
import com.tangem.domain.pay.model.TangemPayOrderInfo
import com.tangem.domain.visa.error.VisaApiError

interface TangemPayReissueCardRepository {

    suspend fun getReissueCardFee(userWalletId: UserWalletId): Either<VisaApiError, TangemPayReissueCardFee>

    suspend fun reissueCard(userWalletId: UserWalletId, cardId: String): Either<VisaApiError, TangemPayOrderInfo>
}