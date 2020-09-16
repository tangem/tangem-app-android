package com.tangem.tap.common.redux.global

import com.tangem.commands.common.network.TangemService
import com.tangem.tap.common.entities.TapCurrency.Companion.DEFAULT_FIAT_CURRENCY
import com.tangem.tap.domain.PayIdManager
import com.tangem.tap.domain.TapWalletManager
import com.tangem.tap.domain.tasks.ScanNoteResponse
import com.tangem.tap.network.coinmarketcap.CoinMarketCapService
import org.rekotlin.StateType
import java.math.BigDecimal

data class GlobalState(
        val scanNoteResponse: ScanNoteResponse? = null,
        val tapWalletManager: TapWalletManager = TapWalletManager(),
        val payIdManager: PayIdManager = PayIdManager(),
        val coinMarketCapService: CoinMarketCapService = CoinMarketCapService(),
        val tangemService: TangemService = TangemService(),
        val conversionRates: ConversionRates = ConversionRates(emptyMap()),
        val appCurrency: FiatCurrencyName = DEFAULT_FIAT_CURRENCY
) : StateType

data class ConversionRates(
        val rates: Map<CryptoCurrencyName, BigDecimal>,
) {

    fun getRate(cryptoCurrency: CryptoCurrencyName): BigDecimal? {
        return rates[cryptoCurrency]
    }
}

typealias CryptoCurrencyName = String
typealias FiatCurrencyName = String

