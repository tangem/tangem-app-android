package com.tangem.datasource.local.visa

import com.tangem.domain.models.pay.TangemPayReissueCardFee
import com.tangem.domain.models.wallet.UserWalletId

interface TangemPayReissueCardStore {

    suspend fun storeReissueFee(userWalletId: UserWalletId, tangemPayReissueCardFee: TangemPayReissueCardFee)

    suspend fun getReissueFee(userWalletId: UserWalletId): TangemPayReissueCardFee?
}