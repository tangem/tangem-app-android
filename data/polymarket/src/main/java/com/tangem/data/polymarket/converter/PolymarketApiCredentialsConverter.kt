package com.tangem.data.polymarket.converter

import com.tangem.data.polymarket.entity.PolymarketApiCredentialsDTO
import com.tangem.domain.polymarket.model.PolymarketApiCredentials

/** Maps the [PolymarketApiCredentials] domain model to its [PolymarketApiCredentialsDTO] storage shape. */
internal fun PolymarketApiCredentials.toDto(): PolymarketApiCredentialsDTO = PolymarketApiCredentialsDTO(
    apiKey = apiKey,
    secret = secret,
    passphrase = passphrase,
)

/** Restores the [PolymarketApiCredentials] domain model from its [PolymarketApiCredentialsDTO] storage shape. */
internal fun PolymarketApiCredentialsDTO.toDomain(): PolymarketApiCredentials = PolymarketApiCredentials(
    apiKey = apiKey,
    secret = secret,
    passphrase = passphrase,
)