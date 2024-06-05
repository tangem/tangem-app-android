package com.tangem.data.staking.converters

import com.tangem.datasource.api.stakekit.models.response.model.TokenWithYield
import com.tangem.domain.staking.model.StakingToken
import com.tangem.domain.staking.model.StakingTokenWithYield
import com.tangem.utils.converter.Converter

class StakingTokenConverter : Converter<TokenWithYield, StakingTokenWithYield> {

    override fun convert(value: TokenWithYield): StakingTokenWithYield {
        return StakingTokenWithYield(
            token = StakingToken(
                name = value.token.name,
                symbol = value.token.symbol,
                decimals = value.token.decimals,
                contractAddress = value.token.address,
                coinGeckoId = value.token.coinGeckoId,
            ),
            availableYieldIds = value.availableYieldIds,
        )
    }
}
