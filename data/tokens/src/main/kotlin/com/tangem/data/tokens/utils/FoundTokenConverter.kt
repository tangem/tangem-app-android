package com.tangem.data.tokens.utils

import com.tangem.datasource.api.tangemTech.models.CoinsResponse
import com.tangem.domain.tokens.model.FoundToken
import com.tangem.utils.converter.Converter

internal object FoundTokenConverter : Converter<CoinsResponse.Coin, FoundToken> {

    override fun convert(value: CoinsResponse.Coin): FoundToken {
        return FoundToken(
            id = value.id,
            name = value.name,
            symbol = value.symbol,
            contractAddress = requireNotNull(value.networks.first().contractAddress),
            decimals = requireNotNull(value.networks.first().decimalCount).intValueExact(),
        )
    }
}