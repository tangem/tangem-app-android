package com.tangem.domain.yield.supply.promo

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.models.YieldBoostPromo
import com.tangem.domain.yield.supply.models.YieldBoostStatus

/**
 * Backend yield-boost promo plumbing.
 *
 * Implementations keep an in-memory cache keyed by [UserWalletId]. On a refresh failure the cached
 * value is returned. With an empty cache the call throws — use cases swallow that to "hide UI".
 */
interface YieldPromoRepository {

    @Throws
    suspend fun getYieldBoostPromo(userWalletId: UserWalletId, forceRefresh: Boolean = false): YieldBoostPromo

    @Throws
    suspend fun getYieldBoostStatus(userWalletId: UserWalletId, forceRefresh: Boolean = false): YieldBoostStatus
}