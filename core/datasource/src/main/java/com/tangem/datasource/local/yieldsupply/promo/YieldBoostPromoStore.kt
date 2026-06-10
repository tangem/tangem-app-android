package com.tangem.datasource.local.yieldsupply.promo

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.models.YieldBoostPromo

interface YieldBoostPromoStore {

    suspend fun getSyncOrNull(userWalletId: UserWalletId): YieldBoostPromo?

    suspend fun store(userWalletId: UserWalletId, value: YieldBoostPromo)
}