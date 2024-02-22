package com.tangem.tap.network.exchangeServices.moonpay

import android.net.Uri
import android.util.Base64
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.services.Result
import com.tangem.common.services.performRequest
import com.tangem.datasource.api.common.createRetrofitInstance
import com.tangem.domain.common.extensions.withIOContext
import com.tangem.tap.common.redux.global.CryptoCurrencyName
import com.tangem.tap.domain.model.Currency
import com.tangem.tap.network.exchangeServices.CurrencyExchangeManager
import com.tangem.tap.network.exchangeServices.ExchangeService
import com.tangem.tap.network.exchangeServices.ExchangeUrlBuilder.Companion.SCHEME
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class MoonPayService(
    private val apiKey: String,
    private val secretKey: String,
    private val logEnabled: Boolean,
) : ExchangeService {

    private val api: MoonPayApi by lazy {
        createRetrofitInstance(
            baseUrl = MoonPayApi.MOOONPAY_BASE_URL,
            logEnabled = logEnabled,
        ).create(MoonPayApi::class.java)
    }

    private var status: MoonPayStatus? = null

    override fun featureIsSwitchedOn(): Boolean = true

    override suspend fun update() {
        withIOContext {
            performRequest {
                val userStatusResult = performRequest { api.getUserStatus(apiKey) }
                if (userStatusResult is Result.Failure) return@performRequest

                val currenciesResult = performRequest { api.getCurrencies(apiKey) }
                if (currenciesResult is Result.Failure) return@performRequest

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
        }
    }

    override fun isBuyAllowed(): Boolean = false

    override fun isSellAllowed(): Boolean {
        return status?.responseUserStatus?.isSellAllowed ?: false
    }

    override fun availableForBuy(currency: Currency): Boolean = false

    override fun availableForSell(currency: Currency): Boolean {
        val availableForSell = status?.availableForSell ?: return false
        val metadata = status?.responseCurrencies?.filter { it.isSellSupported }?.map { it.metadata }
        if (!isSellAllowed()) return false

        return when (currency) {
            is Currency.Blockchain -> {
                val blockchain = currency.blockchain
                when {
                    blockchain.isTestnet() -> false
                    blockchain == Blockchain.Unknown || currency.blockchain == Blockchain.BSC -> false
                    else -> availableForSell.contains(currency.currencySymbol)
                }
            }
            is Currency.Token -> {
                metadata?.any { it?.contractAddress.equals(currency.token.contractAddress, ignoreCase = true) } ?: false
            }
        }
    }

    override fun getUrl(
        action: CurrencyExchangeManager.Action,
        blockchain: Blockchain,
        cryptoCurrencyName: CryptoCurrencyName,
        fatCurrency: String,
        walletAddress: String,
        isDarkTheme: Boolean,
    ): String {
        if (action == CurrencyExchangeManager.Action.Buy) throw UnsupportedOperationException()

        val uri = Uri.Builder()
            .scheme(SCHEME)
            .authority(URL_SELL)
            .appendQueryParameter("apiKey", apiKey)
            .appendQueryParameter("baseCurrencyCode", cryptoCurrencyName)
            .appendQueryParameter("refundWalletAddress", walletAddress)
            .appendQueryParameter("redirectURL", "tangem://sell-request.tangem.com")
        if (isDarkTheme) uri.appendQueryParameter("theme", "dark")

        val originalQuery = uri.build().encodedQuery ?: uri.build().toString()
        val signature = createSignature(originalQuery)
        uri.appendQueryParameter("signature", signature)

        return uri.build().toString()
    }

    override fun getSellCryptoReceiptUrl(action: CurrencyExchangeManager.Action, transactionId: String): String {
        return Uri.Builder()
            .scheme(SCHEME)
            .authority(URL_SELL)
            .appendPath("transaction_receipt")
            .appendQueryParameter("transactionId", transactionId).build().toString()
    }

    private fun createSignature(data: String): String {
        val sha256Hmac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(secretKey.toByteArray(), "HmacSHA256")
        sha256Hmac.init(secretKey)
        val sha256encoded = sha256Hmac.doFinal("?$data".toByteArray())
        return Base64.encodeToString(sha256encoded, Base64.NO_WRAP)
    }

    companion object {
        const val URL_SELL = "sell.moonpay.com"
    }
}

private data class MoonPayStatus(
    val availableForSell: List<String>,
    val responseUserStatus: MoonPayUserStatus,
    val responseCurrencies: List<MoonPayCurrencies>,
)
