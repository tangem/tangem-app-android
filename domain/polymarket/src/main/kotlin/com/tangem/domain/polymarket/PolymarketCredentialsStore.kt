package com.tangem.domain.polymarket

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketApiCredentials

/**
 * Secure storage of the Polymarket CLOB L2 API credentials, keyed by the wallet they belong to.
 *
 * Keyed by [UserWalletId] rather than by the owner EOA so the entry stays reachable when the wallet is gone:
 * deletion removes the wallet before per-wallet data is cleaned up, and the address can only be derived
 * *from* that wallet. The two key spaces are in bijection — one wallet has one seed, hence one owner address
 * on a fixed path — so nothing is lost. The address remains the protocol identity in requests; this is only
 * how the entry is filed locally.
 */
interface PolymarketCredentialsStore {

    /** Persists [credentials] for [userWalletId], replacing any previously stored entry */
    suspend fun store(userWalletId: UserWalletId, credentials: PolymarketApiCredentials)

    /** Returns the credentials stored for [userWalletId] or `null` if there are none */
    suspend fun get(userWalletId: UserWalletId): PolymarketApiCredentials?

    /** Removes the credentials stored for [userWalletId] */
    suspend fun clear(userWalletId: UserWalletId)
}