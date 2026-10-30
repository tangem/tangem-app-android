package com.tangem.domain.polymarket.sessionkey

import com.tangem.domain.models.wallet.UserWalletId
import java.time.Instant

interface PolymarketSessionKeyTradingStore {

    suspend fun isReadyOnExchange(userWalletId: UserWalletId, sessionKeyAddress: String, validUntil: Instant): Boolean

    suspend fun markReadyOnExchange(userWalletId: UserWalletId, sessionKeyAddress: String, validUntil: Instant)

    fun isMissingOnExchange(userWalletId: UserWalletId, sessionKeyAddress: String): Boolean

    /** @param reconciliation [PolymarketSessionKeyReconciliation.Unavailable] keeps the previous one */
    fun setReconciliation(userWalletId: UserWalletId, reconciliation: PolymarketSessionKeyReconciliation)

    fun isRefused(userWalletId: UserWalletId, sessionKeyAddress: String): Boolean

    /** @param sessionKeyAddress its stored readiness is dropped too, so it stays out of trading after a restart */
    suspend fun markRefused(userWalletId: UserWalletId, sessionKeyAddress: String)

    suspend fun clear(userWalletId: UserWalletId)
}