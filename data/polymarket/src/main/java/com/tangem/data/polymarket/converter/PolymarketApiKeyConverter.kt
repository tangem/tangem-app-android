package com.tangem.data.polymarket.converter

import com.tangem.datasource.api.polymarket.clob.models.PolymarketApiKeyResponse
import com.tangem.domain.polymarket.model.PolymarketApiCredentials
import com.tangem.utils.converter.Converter

internal object PolymarketApiKeyConverter : Converter<PolymarketApiKeyResponse, PolymarketApiCredentials> {

    override fun convert(value: PolymarketApiKeyResponse): PolymarketApiCredentials = PolymarketApiCredentials(
        apiKey = value.apiKey,
        secret = value.secret,
        passphrase = value.passphrase,
    )
}