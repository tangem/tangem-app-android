package com.tangem.tap.network.exchangeServices.onramper

import android.net.Uri
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.services.Result
import com.tangem.common.services.performRequest
import com.tangem.tap.common.extensions.urlEncode
import com.tangem.tap.common.redux.global.CryptoCurrencyName
import com.tangem.tap.network.createRetrofitInstance
import com.tangem.tap.network.exchangeServices.CurrencyExchangeManager
import com.tangem.tap.network.exchangeServices.ExchangeService
import com.tangem.tap.network.exchangeServices.ExchangeUrlBuilder
import com.tangem.tap.network.exchangeServices.ExchangeUrlBuilder.Companion.SCHEME
import com.tangem.tap.network.exchangeServices.ExchangeUrlBuilder.Companion.SUCCESS_URL
import kotlinx.coroutines.coroutineScope
import java.util.*

/**
[REDACTED_AUTHOR]
 */
class OnramperService(
    val apiKey: String
) : ExchangeService, ExchangeUrlBuilder {

    private val api: OnramperApi by lazy {
        createRetrofitInstance(OnramperApi.BASE_URL, listOf(AddKeyToHeaderInterceptor(apiKey)))
            .create(OnramperApi::class.java)
    }

    private var status: OnramperStatus? = null

    private suspend fun updateStatus() {
        try {
            coroutineScope {
                val result = performRequest { api.gateways() }
                if (result is Result.Failure) return@coroutineScope result

                val response = (result as Result.Success).data
                val currenciesToBuy = extractCurrenciesToBuy(response).sorted()
                val status = OnramperStatus(currenciesToBuy, response)
                this@OnramperService.status = status
            }
        } catch (error: Error) {
            status = null
            Result.Failure(error)
        }
    }

    private fun extractCurrenciesToBuy(response: GatewaysResponse): List<String> {
        return response.gateways.map { gateway ->
            gateway.cryptoCurrencies.map { currency -> currency.code }
        }.flatten().toMutableSet().toList()
    }

    override suspend fun isBuyAllowed(): Boolean {
        refreshStatus()
        return status != null
    }

    override suspend fun availableToBuy(): List<String> {
        refreshStatus()
        return status?.availableToBuy ?: emptyList()
    }

    private suspend fun refreshStatus() {
        if (status == null) {
            updateStatus()
        }
    }

    override suspend fun isSellAllowed(): Boolean = false

    override suspend fun availableToSell(): List<String> = listOf()

    override fun getUrl(
        action: CurrencyExchangeManager.Action,
        blockchain: Blockchain,
        cryptoCurrencyName: CryptoCurrencyName,
        fiatCurrency: String,
        walletAddress: String,
    ): String? {
        var languageCode = Locale.getDefault().language
        if (languageCode.isEmpty()) languageCode = "en"

        val builder = Uri.Builder()
            .scheme(SCHEME)
            .authority("widget.onramper.com")
            .appendQueryParameter("apiKey", this.apiKey.urlEncode())
            .appendQueryParameter("defaultCrypto", cryptoCurrencyName)
            .appendQueryParameter("wallets", "${blockchain.currency}:$walletAddress".urlEncode())
            .appendQueryParameter("redirectURL", SUCCESS_URL)
            .appendQueryParameter("defaultFiat", fiatCurrency)
            .appendQueryParameter("language", languageCode)

        status?.apply {
            val gateways = responseGateways.gateways.joinToString(",") { it.identifier }.urlEncode()
            builder.appendQueryParameter("onlyGateways", gateways)

        }

        val url = builder.build().toString()
        return url
    }

    override fun getSellCryptoReceiptUrl(action: CurrencyExchangeManager.Action, transactionId: String): String? = null
}

private data class OnramperStatus(
    val availableToBuy: List<String>,
    val responseGateways: GatewaysResponse
)