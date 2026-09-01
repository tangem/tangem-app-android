package com.tangem.domain.polymarket

import com.tangem.domain.models.wallet.UserWalletId

/**
 * Records which wallets the backend has confirmed as ready to trade, so opening the feature again does not have
 * to ask it. Keyed by [UserWalletId] for the same reason as [PolymarketCredentialsStore].
 *
 * Time does not expire an entry, and neither does a setup that is in flight or has failed. The one thing that
 * withdraws it is the backend reporting that the wallet does not exist, which is the shape a backend-side reset
 * takes. An entry is not on its own proof of access, though — the L2 credentials that sign CLOB requests live
 * only on the device that derived them, so the entry decision needs both.
 */
interface PolymarketOnboardedStore {

    /** Whether the backend has confirmed [userWalletId] as ready to trade */
    suspend fun isOnboarded(userWalletId: UserWalletId): Boolean

    /** Records [userWalletId] as confirmed ready to trade */
    suspend fun markOnboarded(userWalletId: UserWalletId)

    /** Removes whatever is recorded for [userWalletId] */
    suspend fun clear(userWalletId: UserWalletId)
}