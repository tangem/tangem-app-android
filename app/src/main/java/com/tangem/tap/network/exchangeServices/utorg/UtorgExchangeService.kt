package com.tangem.tap.network.exchangeServices.utorg

import android.net.Uri
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.services.Result
import com.tangem.common.services.performRequest
import com.tangem.tap.common.redux.global.CryptoCurrencyName
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.network.exchangeServices.CurrencyExchangeManager
import com.tangem.tap.network.exchangeServices.ExchangeService
import com.tangem.tap.network.exchangeServices.utorg.api.RequestSuccessUrl
import com.tangem.tap.network.exchangeServices.utorg.api.model.UtorgCurrencyData
import com.tangem.tap.network.exchangeServices.utorg.api.model.UtorgCurrencyType
import com.tangem.utils.converter.Converter

/**
[REDACTED_AUTHOR]
 */
class UtorgExchangeService(
    private val environment: UtorgEnvironment,
    private val currencyConverter: Converter<UtorgCurrencyData, Blockchain?> = ChainToBlockchainConverter(),
) : ExchangeService {

    private val api = environment.utorgApi

    private val utorgCurrencies = mutableListOf<UtorgCurrencyData>()

    override fun featureIsSwitchedOn(): Boolean = true

    override suspend fun update() {
        when (val result = performRequest { api.getCurrency(environment.apiVersion) }) {
            is Result.Success -> {
                if (!result.data.isSuccess()) return

                val utorgCryptos = result.data.toSuccess().data.filter {
                    it.enabled && it.type == UtorgCurrencyType.CRYPTO
                }
                utorgCurrencies.clear()
                utorgCurrencies.addAll(utorgCryptos)

                performRequest {
                    api.setSuccessUrl(environment.apiVersion, RequestSuccessUrl(environment.successUrl))
                }
            }
            is Result.Failure -> {
                utorgCurrencies.clear()
            }
        }
    }

    override fun isBuyAllowed(): Boolean = true

    override fun isSellAllowed(): Boolean = false

    override fun availableForBuy(currency: Currency): Boolean {
        return true
        if (!isBuyAllowed()) return false

        val foundUtorgCurrency = utorgCurrencies.firstOrNull { utorgCurrency ->
            val utorgBlockchain = currencyConverter.convert(utorgCurrency) ?: return@firstOrNull false

            val isSameBlockchain = utorgBlockchain == currency.blockchain
            val isSameSymbol = utorgCurrency.symbol.lowercase() == currency.currencySymbol.lowercase()
            isSameBlockchain && isSameSymbol
        }

        return foundUtorgCurrency != null
    }

    override fun availableForSell(currency: Currency): Boolean = false

    override fun getUrl(
        action: CurrencyExchangeManager.Action,
        blockchain: Blockchain,
        cryptoCurrencyName: CryptoCurrencyName,
        fiatCurrencyName: String,
        walletAddress: String,
    ): String {
        if (action == CurrencyExchangeManager.Action.Sell) throw UnsupportedOperationException()

        // https://app-stage.utorg.pro/direct/testSID/mvmeaWyWiuVYdZdySgofAt6CmKhJbRhaWA/?&currency=BTC
        val builder = Uri.Builder()
            .scheme(environment.baseUri.scheme)
            .authority(environment.baseUri.authority)
            .appendPath("direct")
            .appendPath(environment.sidValue)
            .appendPath(walletAddress)
            .appendQueryParameter("currency", cryptoCurrencyName)
        // if we set the paymentCurrency, then the Utorg widget didn't work
        // .appendQueryParameter("paymentCurrency", "USD")

        val url = builder.build().toString()
        return url
    }

    override fun getSellCryptoReceiptUrl(action: CurrencyExchangeManager.Action, transactionId: String): String? = null
}

private class ChainToBlockchainConverter : Converter<UtorgCurrencyData, Blockchain?> {
    override fun convert(value: UtorgCurrencyData): Blockchain? {
        return when (value.chain?.uppercase()) {
            "ARBITRUM" -> Blockchain.Arbitrum
            "AVALANCHE" -> Blockchain.Avalanche
            "BINANCE_SMART_CHAIN" -> Blockchain.BSC
            "BITCOIN" -> Blockchain.Bitcoin
            "BNB" -> Blockchain.Binance
            "ETHEREUM" -> Blockchain.Ethereum
            "GNOSIS" -> Blockchain.Gnosis
            "POLYGON" -> Blockchain.Polygon
            "RIPPLE" -> Blockchain.XRP
            "RSK" -> Blockchain.RSK
            "SOLANA" -> Blockchain.Solana
            else -> null
            // "APTOS", "ATOM", "NEAR", "ZKSYNC", "VECHAIN", "VELAS", "VELAS_EVM" -> null
        }
    }
}