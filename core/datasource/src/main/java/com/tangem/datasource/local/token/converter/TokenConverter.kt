package com.tangem.datasource.local.token.converter

import com.tangem.datasource.api.stakekit.models.response.model.TokenDTO
import com.tangem.domain.staking.model.stakekit.Token
import com.tangem.utils.converter.TwoWayConverter

object TokenConverter : TwoWayConverter<TokenDTO, Token> {

    override fun convert(value: TokenDTO): Token {
        return Token(
            name = value.name,
            network = StakingNetworkTypeConverter.convert(value.network),
            symbol = value.symbol,
            decimals = value.decimals,
            address = value.address,
            coinGeckoId = value.coinGeckoId,
            logoURI = value.logoURI,
            isPoints = value.isPoints,
        )
    }

    override fun convertBack(value: Token): TokenDTO {
        return TokenDTO(
            name = value.name,
            network = StakingNetworkTypeConverter.convertBack(value.network),
            symbol = value.symbol,
            decimals = value.decimals,
            address = value.address,
            coinGeckoId = value.coinGeckoId,
            logoURI = value.logoURI,
            isPoints = value.isPoints,
        )
    }
}