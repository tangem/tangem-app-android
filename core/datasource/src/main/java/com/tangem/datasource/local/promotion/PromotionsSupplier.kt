package com.tangem.datasource.local.promotion

import com.tangem.datasource.api.promotion.models.PromotionsResponse
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Shared cache-first fetch of GET /v2/promotion. Keeps one in-memory entry per [UserWalletId]:
 * a non-forced call returns the cached response when present, otherwise it fetches. A fetch failure
 * propagates to the caller (no stale-cache fallback), so the caller decides how to handle it.
 */
interface PromotionsSupplier {

    @Throws(Exception::class)
    suspend fun getPromotions(userWalletId: UserWalletId, forceRefresh: Boolean = false): PromotionsResponse
}