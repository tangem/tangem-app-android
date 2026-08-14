package com.tangem.data.polymarket.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * On-disk representation of the Polymarket L2 API credentials, kept separate from the
 * [com.tangem.domain.polymarket.model.PolymarketApiCredentials] domain model so the storage format can
 * evolve independently of the domain.
 */
@Serializable
internal data class PolymarketApiCredentialsDTO(
    @SerialName("apiKey") val apiKey: String,
    @SerialName("secret") val secret: String,
    @SerialName("passphrase") val passphrase: String,
)