package com.tangem.data.yield.supply.store

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.models.YieldBoostPromo

internal interface YieldBoostPromoStore {

    suspend fun getSyncOrNull(userWalletId: UserWalletId): YieldBoostPromo?

    suspend fun store(userWalletId: UserWalletId, value: YieldBoostPromo)
}