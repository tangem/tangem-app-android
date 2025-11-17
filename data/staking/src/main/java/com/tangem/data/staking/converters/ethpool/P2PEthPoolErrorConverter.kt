package com.tangem.data.staking.converters.ethpool

import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolErrorDetailsDTO
import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolErrorResponse
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.utils.converter.Converter

/**
 * Converter from P2P Error Response to Domain StakingError
 */
@Suppress("MagicNumber")
internal object P2PEthPoolErrorConverter : Converter<P2PEthPoolErrorResponse, StakingError> {

    override fun convert(value: P2PEthPoolErrorResponse): StakingError {
        return convertFromErrorDetails(value.error)
    }

    fun convertFromErrorDetails(details: P2PEthPoolErrorDetailsDTO): StakingError {
        return when (details.code) {
            // Authentication errors
            101111 -> StakingError.UnknownError(
                Exception("Missing Bearer token: ${details.message}"),
            )
            101109 -> StakingError.UnknownError(
                Exception("Invalid Bearer token: ${details.message}"),
            )
            101110 -> StakingError.UnknownError(
                Exception("Server authorization error: ${details.message}"),
            )

            // Validation errors
            100101 -> StakingError.InvalidAmount(details.message)

            // Withdrawal errors
            127104 -> StakingError.DataError(
                IllegalStateException("No withdrawable balance: ${details.message}"),
            )
            127105 -> StakingError.UnknownError(
                Exception("Gas amount too low: ${details.message}"),
            )
            127106 -> StakingError.InvalidAmount("Invalid delegator address: ${details.message}")
            127107 -> StakingError.UnknownError(
                Exception("Gas price too low: ${details.message}"),
            )
            127108 -> StakingError.UnknownError(
                Exception("Transaction simulation failed: ${details.message}"),
            )

            // Vault errors
            127101 -> StakingError.DataError(
                IllegalStateException("Invalid vault: ${details.message}"),
            )

            // Account errors
            124108 -> StakingError.DataError(
                IllegalStateException("Invalid delegator: ${details.message}"),
            )

            // Network errors
            127100 -> StakingError.DataError(
                IllegalStateException("Unsupported network: ${details.message}"),
            )

            else -> StakingError.UnknownError(
                Exception("${details.code}: ${details.message}"),
            )
        }
    }
}