package com.tangem.tap.network.exchangeServices

import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.tokens.model.CryptoCurrency

class DefaultRampManager(private val exchangeService: ExchangeService?) : RampStateManager {

    private val cryptoCurrencyConverter = CryptoCurrencyConverter()
    override fun availableForBuy(scanResponse: ScanResponse, cryptoCurrency: CryptoCurrency): Boolean {
        return exchangeService?.availableForBuy(
            scanResponse,
            currency = cryptoCurrencyConverter.convertBack(cryptoCurrency),
        ) ?: false
    }

    override fun availableForSell(cryptoCurrency: CryptoCurrency): Boolean {
        return exchangeService?.availableForSell(
            currency = cryptoCurrencyConverter.convertBack(cryptoCurrency),
        ) ?: false
    }
}
