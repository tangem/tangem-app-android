package com.tangem.domain.express.models

import kotlinx.serialization.Serializable

/**
 * Express asset model.
 *
 * @property id The unique identifier of the Express asset.
 * @property isExchangeAvailable Indicates if exchange is available for this asset.
 * @property isOnrampAvailable Indicates if onramp is available for this asset (nullable).
 *
[REDACTED_AUTHOR]
 */
@Serializable
data class ExpressAsset(
    val id: ID,
    val isExchangeAvailable: Boolean,
    val isOnrampAvailable: Boolean?,
) {

    /**
     * Unique identifier for an Express asset, consisting of network ID and contract address.
     *
     * @property networkId The network ID of the asset.
     * @property contractAddress The contract address of the asset.
     */
    @Serializable
    data class ID(val networkId: String, val contractAddress: String) {

        companion object {

            /**
             * Creates an [ID] instance, defaulting the contract address to "0" if null.
             *
             * @param networkId The network ID of the asset.
             * @param contractAddress The contract address of the asset, or null to default to "0".
             * @return An [ID] instance with the specified network ID and contract address.
             */
            operator fun invoke(networkId: String, contractAddress: String?): ID {
                return ID(networkId = networkId, contractAddress = contractAddress ?: EMPTY_CONTRACT_ADDRESS_VALUE)
            }
        }
    }

    companion object {
        const val EMPTY_CONTRACT_ADDRESS_VALUE = "0"
    }
}