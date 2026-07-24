package com.tangem.domain.polymarket

import com.tangem.domain.polymarket.model.PolymarketApiCredentials

/**
 * Secure storage of the Polymarket CLOB L2 API credentials, keyed by the owner EOA address.
 *
 * Implementations must treat the address case-insensitively: an EIP-55 checksummed address and its
 * lowercase form refer to the same entry.
 */
interface PolymarketCredentialsStore {

    /** Persists [credentials] for [ownerAddress], replacing any previously stored entry */
    suspend fun store(ownerAddress: String, credentials: PolymarketApiCredentials)

    /** Returns the credentials stored for [ownerAddress] or `null` if there are none */
    suspend fun get(ownerAddress: String): PolymarketApiCredentials?

    /** Removes the credentials stored for [ownerAddress] */
    suspend fun clear(ownerAddress: String)
}