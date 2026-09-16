package com.tangem.data.yield.supply.store

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.models.YieldBoostStatus

internal interface YieldBoostStatusStore {

    suspend fun getSyncOrNull(userWalletId: UserWalletId): YieldBoostStatus?

    suspend fun store(userWalletId: UserWalletId, value: YieldBoostStatus)
}