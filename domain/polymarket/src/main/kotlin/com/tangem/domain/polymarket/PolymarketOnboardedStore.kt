package com.tangem.domain.polymarket

import com.tangem.domain.models.wallet.UserWalletId

/**
 * Records which wallets the backend has confirmed as ready to trade, so opening the feature again does not have
 * to ask it. Keyed by [UserWalletId] for the same reason as [PolymarketCredentialsStore].
 *
 * Nothing expires an entry: a wallet the backend called ready never becomes un-ready. It is not on its own proof
 * of access, though — the L2 credentials that sign CLOB requests live only on the device that derived them, so
 * the entry decision needs both.
 */
interface PolymarketOnboardedStore {

    /** Whether the backend has confirmed [userWalletId] as ready to trade */
    suspend fun isOnboarded(userWalletId: UserWalletId): Boolean

    /** Records [userWalletId] as confirmed ready to trade */
    suspend fun markOnboarded(userWalletId: UserWalletId)

    /** Removes whatever is recorded for [userWalletId] */
    suspend fun clear(userWalletId: UserWalletId)
}