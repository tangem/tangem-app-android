package com.tangem.data.walletmanager.utils

import com.tangem.blockchain.common.CryptoCurrencyType
import com.tangem.blockchain.common.Token
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.utils.converter.Converter

internal class CryptoCurrencyTypeConverter : Converter<CryptoCurrency, CryptoCurrencyType> {
    override fun convert(value: CryptoCurrency): CryptoCurrencyType {
        return when (value) {
            is CryptoCurrency.Coin -> CryptoCurrencyType.Coin
            is CryptoCurrency.Token -> CryptoCurrencyType.Token(
                info = Token(
                    name = value.name,
                    symbol = value.symbol,
                    contractAddress = value.contractAddress,
                    decimals = value.decimals,
                    id = value.id.rawCurrencyId?.value,
                ),
            )
        }
    }
}