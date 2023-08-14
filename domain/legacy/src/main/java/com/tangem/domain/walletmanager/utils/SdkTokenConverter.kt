package com.tangem.domain.walletmanager.utils

import com.tangem.domain.tokens.models.CryptoCurrency
import com.tangem.utils.converter.Converter
import com.tangem.blockchain.common.Token as SdkToken

internal class SdkTokenConverter : Converter<CryptoCurrency.Token, SdkToken> {

    override fun convert(value: CryptoCurrency.Token): SdkToken {
        return SdkToken(
            id = value.id.rawCurrencyId,
            name = value.name,
            symbol = value.symbol,
            contractAddress = value.contractAddress,
            decimals = value.decimals,
        )
    }
}
