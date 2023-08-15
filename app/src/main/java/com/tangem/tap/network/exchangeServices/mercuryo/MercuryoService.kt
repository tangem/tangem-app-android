package com.tangem.tap.network.exchangeServices.mercuryo

import android.net.Uri
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.calculateSha512
import com.tangem.common.extensions.toHexString
import com.tangem.common.services.Result
import com.tangem.common.services.performRequest
import com.tangem.tap.common.redux.global.CryptoCurrencyName
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.network.exchangeServices.CurrencyExchangeManager
import com.tangem.tap.network.exchangeServices.ExchangeService
import com.tangem.tap.network.exchangeServices.ExchangeUrlBuilder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
* [REDACTED_AUTHOR]
 */
internal class MercuryoService(private val environment: MercuryoEnvironment) : ExchangeService {

    private val api: MercuryoApi = environment.mercuryoApi

    private val blockchainsAvailableToBuy = CopyOnWriteArrayList<Blockchain>()
    private val tokensAvailableToBuy = ConcurrentHashMap<String, List<Blockchain>>()

    override fun featureIsSwitchedOn(): Boolean = true

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
                    else -> blockchainsAvailableToBuy.contains(blockchain)
                }
            }

            is Currency.Token -> {
                val supportedInBlockchains = tokensAvailableToBuy[currency.currencySymbol] ?: return false
                supportedInBlockchains.contains(blockchain)
            }
        }
    }

    override fun availableForSell(currency: Currency): Boolean = false

    override suspend fun update() {
        val result = performRequest { api.currencies(environment.apiVersion) }
        when {
            result is Result.Success && result.data.status == RESPONSE_SUCCESS_STATUS_CODE -> {
                handleSuccessfullyUpdatedData(data = result.data.data)
            }
            result is Result.Failure -> {
                blockchainsAvailableToBuy.clear()
                tokensAvailableToBuy.clear()
            }
        }
    }

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
            .appendQueryParameter("widget_id", environment.widgetId)
            .appendQueryParameter("type", action.name.lowercase())
            .appendQueryParameter("currency", cryptoCurrencyName)
            .appendQueryParameter("address", walletAddress)
            .appendQueryParameter("signature", signature(walletAddress))
            .appendQueryParameter("fix_currency", "true")
            .appendQueryParameter("return_url", ExchangeUrlBuilder.SUCCESS_URL)

        return builder.build().toString()
    }

    override fun getSellCryptoReceiptUrl(action: CurrencyExchangeManager.Action, transactionId: String): String? = null

    private fun handleSuccessfullyUpdatedData(data: MercuryoCurrenciesResponse.Data) {
        data.crypto.forEach { currencyName ->
            val blockchain = blockchainFromCurrencyName(currencyName)
            if (blockchain == null) {
                val specificBlockchain = data.config.base[currencyName]?.let(::blockchainFromCurrencyName)
                if (specificBlockchain != null) {
                    tokensAvailableToBuy.set(
                        key = currencyName,
                        value = tokensAvailableToBuy[currencyName].orEmpty() + specificBlockchain,
                    )
                }
            } else {
                blockchainsAvailableToBuy.add(blockchain)
            }
        }
    }

    private fun blockchainFromCurrencyName(currencyName: String): Blockchain? {
        return when (currencyName) {
            "BNB" -> Blockchain.BSC
            "ETH" -> Blockchain.Ethereum
            "ADA" -> Blockchain.Cardano
            else -> Blockchain.values().find { it.currency.lowercase() == currencyName.lowercase() }
        }
    }

    private fun signature(address: String) = (address + environment.secret).calculateSha512().toHexString().lowercase()

    private companion object {
        const val RESPONSE_SUCCESS_STATUS_CODE = 200
    }
}
