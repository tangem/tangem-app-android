package com.tangem.feature.referral.domain.converter

import com.tangem.feature.referral.domain.models.TokenData
import com.tangem.lib.crypto.models.Currency
import com.tangem.lib.crypto.models.NativeToken
import com.tangem.lib.crypto.models.NonNativeToken
import com.tangem.utils.converter.Converter

import javax.inject.Inject

class TokensConverter @Inject constructor() : Converter<TokenData, Currency> {

    override fun convert(value: TokenData): Currency {
        return if (value.decimalCount != null &&
            value.contractAddress != null
        ) {
            NonNativeToken(
                id = value.id,
                name = value.name,
                symbol = value.symbol,
                networkId = value.networkId,
                contractAddress = value.contractAddress,
                decimalCount = value.decimalCount,
            )
        } else {
            NativeToken(
                id = value.id,
                name = value.name,
                symbol = value.symbol,
                networkId = value.networkId,
            )
        }
    }
}
