package com.tangem.data.earn.converter

import com.tangem.datasource.api.tangemTech.models.EarnResponse
import com.tangem.domain.models.earn.EarnToken
import com.tangem.utils.converter.Converter

internal object EarnTokenConverter : Converter<EarnResponse, EarnToken> {

    override fun convert(value: EarnResponse): EarnToken {
        return EarnToken(
            apy = value.apy,
            networkId = value.networkId,
            rewardType = value.rewardType,
            type = value.type,
            tokenId = value.token.id,
            tokenSymbol = value.token.symbol,
            tokenName = value.token.name,
            tokenAddress = value.token.address,
        )
    }
}