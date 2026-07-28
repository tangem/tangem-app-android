package com.tangem.datasource.local.visa

import com.tangem.domain.models.pay.TangemPayReissueCardFee
import com.tangem.domain.models.wallet.UserWalletId

interface TangemPayReissueCardStore {

    suspend fun storeReissueFee(userWalletId: UserWalletId, tangemPayReissueCardFee: TangemPayReissueCardFee)

    suspend fun getReissueFee(userWalletId: UserWalletId): TangemPayReissueCardFee?

    suspend fun storeReissueOrderId(cardId: String, orderId: String)

    suspend fun removeReissueOrderId(cardId: String)

    suspend fun getOrderId(cardId: String): String?
}