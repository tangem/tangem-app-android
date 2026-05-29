package com.tangem.domain.yield.supply

import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Resolves the yield-module proxy address for a `(wallet, network)` pair and caches the result.
 *
 * The address is derived from on-chain state (factory contract + user's wallet) and is stable
 * for the lifetime of the wallet, so caching the result avoids redundant blockchain calls.
 *
 * Call [invalidate] when the wallet's yield-module state may have changed (e.g. after a
 * successful upgrade or removal of yield-supply).
 */
interface YieldModuleAddressProvider {

    /**
     * Returns the yield-module proxy address, or `null` if the address is currently unavailable
     * (e.g. RPC failure inside the SDK).
     */
    suspend fun getOrFetch(userWalletId: UserWalletId, network: Network): String?

    /** Drops cached entries for [userWalletId], or the entire cache when [userWalletId] is `null`. */
    fun invalidate(userWalletId: UserWalletId? = null)
}