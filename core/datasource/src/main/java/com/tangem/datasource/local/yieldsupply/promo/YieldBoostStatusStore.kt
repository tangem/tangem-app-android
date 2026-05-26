package com.tangem.datasource.local.yieldsupply.promo

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.models.YieldBoostStatus

interface YieldBoostStatusStore {

    suspend fun getSyncOrNull(userWalletId: UserWalletId): YieldBoostStatus?

    suspend fun store(userWalletId: UserWalletId, value: YieldBoostStatus)
}