package com.tangem.datasource.local.token.converter

import com.tangem.datasource.api.stakekit.models.response.model.TokenDTO
import com.tangem.domain.models.staking.YieldToken
import com.tangem.utils.converter.TwoWayConverter

object YieldTokenConverter : TwoWayConverter<TokenDTO, YieldToken> {

    override fun convert(value: TokenDTO): YieldToken {
        return YieldToken(
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

    override fun convertBack(value: YieldToken): TokenDTO {
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