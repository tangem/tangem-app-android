package com.tangem.tap.network.exchangeServices

import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.tokens.model.CryptoCurrency

class DefaultRampManager(private val exchangeService: ExchangeService?) : RampStateManager {

    private val cryptoCurrencyConverter = CryptoCurrencyConverter()
    override fun availableForBuy(cryptoCurrency: CryptoCurrency): Boolean {
        return exchangeService?.availableForBuy(
            currency = cryptoCurrencyConverter.convertBack(cryptoCurrency),
        ) ?: false
    }

    override fun availableForSell(cryptoCurrency: CryptoCurrency): Boolean {
        return exchangeService?.availableForSell(
            currency = cryptoCurrencyConverter.convertBack(cryptoCurrency),
        ) ?: false
    }
}
