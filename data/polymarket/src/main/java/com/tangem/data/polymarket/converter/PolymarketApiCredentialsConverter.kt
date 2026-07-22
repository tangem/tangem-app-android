package com.tangem.data.polymarket.converter

import com.tangem.data.polymarket.entity.PolymarketApiCredentialsDTO
import com.tangem.domain.polymarket.model.PolymarketApiCredentials
import com.tangem.utils.converter.TwoWayConverter

/**
 * Maps between the [PolymarketApiCredentials] domain model and the [PolymarketApiCredentialsDTO]
 * storage DTO. `convert` produces the on-disk shape; `convertBack` restores the domain model.
 */
internal object PolymarketApiCredentialsConverter :
    TwoWayConverter<PolymarketApiCredentials, PolymarketApiCredentialsDTO> {

    override fun convert(value: PolymarketApiCredentials): PolymarketApiCredentialsDTO = PolymarketApiCredentialsDTO(
        apiKey = value.apiKey,
        secret = value.secret,
        passphrase = value.passphrase,
    )

    override fun convertBack(value: PolymarketApiCredentialsDTO): PolymarketApiCredentials = PolymarketApiCredentials(
        apiKey = value.apiKey,
        secret = value.secret,
        passphrase = value.passphrase,
    )
}