package com.tangem.tap.network.exchangeServices.mercuryo

import android.net.Uri
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.calculateSha512
import com.tangem.common.extensions.toHexString
import com.tangem.common.services.Result
import com.tangem.common.services.performRequest
import com.tangem.datasource.api.common.createRetrofitInstance
import com.tangem.tap.common.redux.global.CryptoCurrencyName
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.network.exchangeServices.CurrencyExchangeManager
import com.tangem.tap.network.exchangeServices.ExchangeService
import com.tangem.tap.network.exchangeServices.ExchangeUrlBuilder

/**
 * Created by Anton Zhilenkov on 06/07/2022.
 */
class MercuryoService(
    private val apiVersion: String,
    private val mercuryoWidgetId: String,
    private val secret: String,
    private val logEnabled: Boolean,
) : ExchangeService, ExchangeUrlBuilder {

    private val api: MercuryoApi = createRetrofitInstance(
        baseUrl = MercuryoApi.BASE_URL,
        logEnabled = logEnabled,
    ).create(MercuryoApi::class.java)

    private val blockchainsAvailableToBuy = mutableListOf<Blockchain>()
    private val tokensAvailableToBy = mutableMapOf<String, MutableList<Blockchain>>()

    override fun featureIsSwitchedOn(): Boolean = true

    @Suppress("NestedBlockDepth")
    override suspend fun update() {
        when (val result = performRequest { api.currencies(apiVersion) }) {
            is Result.Success -> {
                val response = result.data
                if (response.status == RESPONSE_SUCCESS_STATUS_CODE) {
                    // all currencies which can be bought
                    val currenciesAvailableToBy = response.data.crypto
                    // tokens which can be bought only from specific blockchain network
                    val supportedTokensWithNetwork = response.data.config.base

                    currenciesAvailableToBy.forEach { currencyName ->
                        val blockchain = blockchainFromCurrencyName(currencyName)
                        if (blockchain == null) {
                            // suppose its a token
                            supportedTokensWithNetwork[currencyName]?.let {
                                blockchainFromCurrencyName(it)
                            }?.let { blockchainNetwork ->
                                val supportedInBlockchainsNetwork = tokensAvailableToBy[currencyName]
                                    ?: mutableListOf()
                                supportedInBlockchainsNetwork.add(blockchainNetwork)
                                tokensAvailableToBy[currencyName] = supportedInBlockchainsNetwork
                            }
                        } else {
                            blockchainsAvailableToBuy.add(blockchain)
                        }
                    }
                }
            }
            is Result.Failure -> {
                blockchainsAvailableToBuy.clear()
                tokensAvailableToBy.clear()
            }
        }
    }

    override fun isBuyAllowed(): Boolean = true

    override fun isSellAllowed(): Boolean = false

    override fun availableForBuy(currency: Currency): Boolean {
        if (!isBuyAllowed()) return false

        // blockchains which cant be defined by mercuryo service
        val unsupportedBlockchains = listOf(
            Blockchain.Unknown,
            Blockchain.Binance,
            Blockchain.Arbitrum,
            Blockchain.Optimism,
        )
        val blockchain = currency.blockchain

        return when (currency) {
            is Currency.Blockchain -> {
                when {
                    blockchain.isTestnet() -> blockchain.getTestnetTopUpUrl() != null
                    unsupportedBlockchains.contains(blockchain) -> false
                    else -> {
                        blockchainsAvailableToBuy.contains(currency.blockchain)
                    }
                }
            }
            is Currency.Token -> {
                val supportedInBlockchains = tokensAvailableToBy[currency.currencySymbol] ?: return false
                supportedInBlockchains.contains(currency.blockchain)
            }
        }
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

        val builder = Uri.Builder()
            .scheme(ExchangeUrlBuilder.SCHEME)
            .authority("exchange.mercuryo.io")
            .appendQueryParameter("widget_id", mercuryoWidgetId)
            .appendQueryParameter("type", action.name.lowercase())
            .appendQueryParameter("currency", cryptoCurrencyName)
            .appendQueryParameter("address", walletAddress)
            .appendQueryParameter("signature", signature(walletAddress))
            .appendQueryParameter("fix_currency", "true")
            .appendQueryParameter("return_url", ExchangeUrlBuilder.SUCCESS_URL)

        val url = builder.build().toString()
        return url
    }

    private fun signature(address: String): String {
        return (address + secret).calculateSha512().toHexString().lowercase()
    }

    override fun getSellCryptoReceiptUrl(
        action: CurrencyExchangeManager.Action,
        transactionId: String,
    ): String? = null

    private fun blockchainFromCurrencyName(currencyName: String): Blockchain? = when (currencyName) {
        "BNB" -> Blockchain.BSC
        "ETH" -> Blockchain.Ethereum
        "ADA" -> Blockchain.CardanoShelley
        else -> Blockchain.values().find { it.currency.lowercase() == currencyName.lowercase() }
    }

    companion object {
        private const val RESPONSE_SUCCESS_STATUS_CODE = 200
    }
}
