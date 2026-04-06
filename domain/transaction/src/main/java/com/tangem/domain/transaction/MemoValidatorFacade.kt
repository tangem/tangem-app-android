package com.tangem.domain.transaction

import com.tangem.domain.models.network.Network

/**
 * Facade for memo validation operations.
 */
interface MemoValidatorFacade {

    /**
     * Returns true if a memo/destination tag is required for the given [destinationAddress] on [network].
     * Returns false on network errors or for unsupported blockchains.
     */
    suspend fun isMemoRequired(network: Network, destinationAddress: String): Boolean

    /**
     * Returns true if [memo] is valid for the given [network], or if memo is not supported.
     * Returns true on errors (lenient fallback).
     */
    suspend fun validateMemo(network: Network, memo: String): Boolean
}