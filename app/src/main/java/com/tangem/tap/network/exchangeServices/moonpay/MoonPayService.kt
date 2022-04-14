package com.tangem.tap.network.exchangeServices.moonpay

import android.net.Uri
import android.util.Base64
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.services.Result
import com.tangem.common.services.performRequest
import com.tangem.network.common.createRetrofitInstance
import com.tangem.tap.common.extensions.urlEncode
import com.tangem.tap.common.redux.global.CryptoCurrencyName
import com.tangem.tap.network.exchangeServices.CurrencyExchangeManager
import com.tangem.tap.network.exchangeServices.ExchangeService
import com.tangem.tap.network.exchangeServices.ExchangeUrlBuilder
import com.tangem.tap.network.exchangeServices.ExchangeUrlBuilder.Companion.SCHEME
import com.tangem.tap.network.exchangeServices.ExchangeUrlBuilder.Companion.URL_SELL
import kotlinx.coroutines.coroutineScope
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class MoonPayService(
    private val apiKey: String,
    private val secretKey: String,
) : ExchangeService, ExchangeUrlBuilder {

    private val api: MoonPayApi by lazy {
        createRetrofitInstance(MoonPayApi.MOOONPAY_BASE_URL)
            .create(MoonPayApi::class.java)
    }

    private var status: MoonPayStatus? = null

    private suspend fun updateStatus() {
        try {
            coroutineScope {
                val userStatusResult = performRequest { api.getUserStatus(apiKey) }
                if (userStatusResult is Result.Failure) return@coroutineScope userStatusResult

                val currenciesResult = performRequest { api.getCurrencies(apiKey) }
                if (currenciesResult is Result.Failure) return@coroutineScope currenciesResult

                val userStatus = (userStatusResult as Result.Success).data
                val currencies = (currenciesResult as Result.Success).data

//                val currenciesToBuy = mutableListOf<String>()
                val currenciesToSell = mutableListOf<String>()

                currencies.forEach { currencyStatus ->
                    if (currencyStatus.type != "crypto" || currencyStatus.isSuspended ||
                        !currencyStatus.supportsLiveMode
                    ) {
                        return@forEach
                    }

                    if (userStatus.countryCode == "USA") {
                        if (!currencyStatus.isSupportedInUS) return@forEach
                        if (currencyStatus.notAllowedUSStates.contains(userStatus.stateCode)) return@forEach
                    }
                    val currencyCode = currencyStatus.code.uppercase()
//                    currenciesToBuy.add(currencyCode)

                    if (currencyStatus.isSellSupported) currenciesToSell.add(currencyCode)
                }
//                currenciesToBuy.sort()
                currenciesToSell.sort()

                status = MoonPayStatus(currenciesToSell, userStatus, currencies)
            }
        } catch (error: Error) {
            status = null
            Result.Failure(error)
        }
    }

    override suspend fun isBuyAllowed(): Boolean = false

    override suspend fun availableToBuy(): List<String> = listOf()

    override suspend fun isSellAllowed(): Boolean {
        refreshStatus()
        return status?.responseUserStatus?.isSellAllowed ?: false
    }

    override suspend fun availableToSell(): List<String> {
        refreshStatus()
        return status?.availableToSell ?: emptyList()
    }

    private suspend fun refreshStatus() {
        if (status == null) {
            updateStatus()
        }
    }

    override fun getUrl(
        action: CurrencyExchangeManager.Action,
        blockchain: Blockchain,
        cryptoCurrencyName: CryptoCurrencyName,
        fatCurrency: String,
        walletAddress: String
    ): String? {
        if (action == CurrencyExchangeManager.Action.Buy) throw UnsupportedOperationException()

        val uri = Uri.Builder()
            .scheme(SCHEME)
            .authority(URL_SELL)
            .appendQueryParameter("apiKey", apiKey.urlEncode())
            .appendQueryParameter("baseCurrencyCode", cryptoCurrencyName.urlEncode())
            .appendQueryParameter("refundWalletAddress", walletAddress.urlEncode())
            .appendQueryParameter("redirectURL", "tangem://sell-request.tangem.com".urlEncode())

        val originalQuery = uri.build().toString()
        val signature = createSignature(originalQuery)
        uri.appendQueryParameter("signature", signature.urlEncode())

        val url = uri.build().toString()
        return url
    }

    override fun getSellCryptoReceiptUrl(action: CurrencyExchangeManager.Action, transactionId: String): String? {
        val url = Uri.Builder()
            .scheme(SCHEME)
            .authority(URL_SELL)
            .appendPath("transaction_receipt")
            .appendQueryParameter("transactionId", transactionId).build().toString()
        return url

    }

    private fun createSignature(data: String): String {
        val sha256Hmac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(secretKey.toByteArray(), "HmacSHA256")
        sha256Hmac.init(secretKey)
        val sha256encoded = sha256Hmac.doFinal(data.toByteArray())
        return Base64.encodeToString(sha256encoded, Base64.NO_WRAP)
    }
}

private data class MoonPayStatus(
    val availableToSell: List<String>,
    val responseUserStatus: MoonPayUserStatus,
    val responseCurrencies: List<MoonPayCurrencies>
)