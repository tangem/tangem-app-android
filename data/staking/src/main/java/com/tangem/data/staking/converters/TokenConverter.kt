package com.tangem.data.staking.converters

import com.tangem.datasource.api.stakekit.models.response.model.TokenDTO
import com.tangem.domain.staking.model.Token
import com.tangem.utils.converter.Converter

class TokenConverter(
    private val stakingNetworkTypeConverter: StakingNetworkTypeConverter,
) : Converter<TokenDTO, Token> {

    override fun convert(value: TokenDTO): Token {
        return Token(
            name = value.name,
            network = stakingNetworkTypeConverter.convert(value.network),
            symbol = value.symbol,
            decimals = value.decimals,
            address = value.address,
            coinGeckoId = value.coinGeckoId,
            logoURI = value.logoURI,
            isPoints = value.isPoints,
        )
    }
}