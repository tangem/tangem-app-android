package com.tangem.domain.polymarket.sessionkey

import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface PolymarketFastTradingBannerStore {

    fun dismissedAt(userWalletId: UserWalletId, kind: PolymarketFastTradingBannerKind): Flow<Instant?>

    suspend fun dismiss(userWalletId: UserWalletId, kind: PolymarketFastTradingBannerKind, at: Instant)

    suspend fun clear(userWalletId: UserWalletId)
}