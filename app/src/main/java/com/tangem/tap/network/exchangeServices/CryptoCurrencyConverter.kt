package com.tangem.tap.network.exchangeServices

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.tap.domain.model.Currency
import com.tangem.utils.converter.Converter

internal object CryptoCurrencyConverter : Converter<CryptoCurrency, Currency> {

    override fun convert(value: CryptoCurrency): Currency {
        val blockchain = value.network.toBlockchain()
        if (blockchain == Blockchain.Unknown) error("CryptoCurrencyConverter convertBack Unknown blockchain")
        return when (value) {
            is CryptoCurrency.Coin -> Currency.Blockchain(
                blockchain = blockchain,
                derivationPath = value.network.derivationPath.value,
            )
            is CryptoCurrency.Token -> Currency.Token(
                token = Token(
                    name = value.name,
                    symbol = value.symbol,
                    contractAddress = value.contractAddress,
                    decimals = value.decimals,
                    id = value.id.value,
                ),
                blockchain = blockchain,
                derivationPath = value.network.derivationPath.value,
            )
        }
    }
}