package com.tangem.feature.referral.domain.converter

import com.tangem.crypto.models.Token
import com.tangem.feature.referral.domain.models.TokenData
import com.tangem.utils.converter.Converter

class TokensConverter : Converter<TokenData, Token> {

    override fun convert(value: TokenData): Token {
        return Token(
            id = value.id,
            name = value.name,
            symbol = value.symbol,
            networkId = value.networkId,
            contractAddress = value.contractAddress,
            decimalCount = value.decimalCount,
        )
    }
}