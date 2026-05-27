package com.tangem.domain.walletconnect.usecase.method

import arrow.core.Either
import com.tangem.domain.walletconnect.model.HandleMethodError

/**
 * Base use case for WalletConnect methods that return wallet addresses.
 *
 * This is a non-signing operation that returns addresses immediately.
 */
interface WcGetAddressesUseCase : WcMethodUseCase, WcMethodContext {

    /**
     * Get wallet addresses.
     *
     * @return Either error or list of addresses with their metadata
     */
    suspend operator fun invoke(): Either<HandleMethodError, GetAddressesResult>

    /**
     * Reject the request.
     */
    fun reject()

    /**
     * Result containing wallet addresses.
     */
    data class GetAddressesResult(
        val addresses: List<AddressInfo>,
    )

    /**
     * Address information.
     */
    data class AddressInfo(
        val address: String,
        val publicKey: String?,
        val path: String?,
        val intention: String?,
    )
}