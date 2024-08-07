package com.tangem.tap.network.exchangeServices.moonpay

import android.net.Uri
import android.util.Base64
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.services.Result
import com.tangem.common.services.performRequest
import com.tangem.datasource.api.common.createRetrofitInstance
import com.tangem.domain.common.extensions.withIOContext
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.tap.domain.model.Currency
import com.tangem.tap.network.exchangeServices.CurrencyExchangeManager
import com.tangem.tap.network.exchangeServices.ExchangeService
import com.tangem.tap.network.exchangeServices.ExchangeUrlBuilder.Companion.SCHEME
import com.tangem.tap.network.exchangeServices.moonpay.models.MoonPayAvailableCurrency
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
                val userStatus = when (val result = performRequest { api.getUserStatus(apiKey) }) {
                    is Result.Failure -> return@performRequest
                    is Result.Success -> result.data
                }

                val currencies = when (val result = performRequest { api.getCurrencies(apiKey) }) {
                    is Result.Failure -> return@performRequest
                    is Result.Success -> result.data
                }

                val currenciesToSell = currencies
                    .filter { currency ->
                        checkGeneralRequirements(currency) && checkUSARequirements(userStatus, currency)
                    }
                    .mapNotNull { currency ->
                        if (!currency.isSellSupported) return@mapNotNull null
                        MoonPayAvailableCurrency(
                            currencyCode = currency.code,
                            networkCode = currency.metadata?.networkCode ?: return@mapNotNull null,
                            contractAddress = currency.metadata.contractAddress,
                        )
                    }

                status = MoonPayStatus(currenciesToSell, userStatus, currencies)
            }
        }
    }

    private fun checkGeneralRequirements(currency: MoonPayCurrencies): Boolean {
        return currency.type == "crypto" && !currency.isSuspended && currency.supportsLiveMode &&
            currency.isSellSupported
    }

    private fun checkUSARequirements(userStatus: MoonPayUserStatus, currency: MoonPayCurrencies): Boolean {
        return if (userStatus.countryCode == "USA") {
            currency.isSupportedInUS && !currency.notAllowedUSStates.contains(userStatus.stateCode)
        } else {
            true
        }
    }

    override fun isBuyAllowed(): Boolean = false

    override fun isSellAllowed(): Boolean {
        return status?.responseUserStatus?.isSellAllowed ?: false
    }

    override fun availableForBuy(scanResponse: ScanResponse, currency: Currency): Boolean = false

    override fun availableForSell(currency: Currency): Boolean {
        if (!isSellAllowed()) return false

        val availableForSell = status?.availableForSell ?: return false

        val supportedCurrency = currency.blockchain.moonPaySupportedCurrency ?: return false
        return availableForSell.any {
            when (currency) {
                is Currency.Blockchain -> {
                    it.networkCode.equals(other = supportedCurrency.networkCode, ignoreCase = true) &&
                        it.currencyCode.equals(other = supportedCurrency.currencyCode, ignoreCase = true)
                }
                is Currency.Token -> {
                    it.networkCode.equals(other = supportedCurrency.networkCode, ignoreCase = true) &&
                        it.contractAddress.equals(other = currency.token.contractAddress, ignoreCase = true)
                }
            }
        }
    }

    override fun getUrl(
        action: CurrencyExchangeManager.Action,
        cryptoCurrency: CryptoCurrency,
        fiatCurrencyName: String,
        walletAddress: String,
        isDarkTheme: Boolean,
    ): String? {
        if (action == CurrencyExchangeManager.Action.Buy) throw UnsupportedOperationException()

        val blockchain = Blockchain.fromId(cryptoCurrency.network.id.value)
        val supportedCurrency = blockchain.moonPaySupportedCurrency ?: return null
        val moonpayCurrency = status?.availableForSell?.firstOrNull {
            when (cryptoCurrency) {
                is CryptoCurrency.Coin -> {
                    it.networkCode.equals(other = supportedCurrency.networkCode, ignoreCase = true) &&
                        it.currencyCode.equals(other = supportedCurrency.currencyCode, ignoreCase = true)
                }
                is CryptoCurrency.Token -> {
                    it.networkCode.equals(other = supportedCurrency.networkCode, ignoreCase = true) &&
                        it.contractAddress.equals(other = cryptoCurrency.contractAddress, ignoreCase = true)
                }
            }
        } ?: return null

        val uri = Uri.Builder()
            .scheme(SCHEME)
            .authority(URL_SELL)
            .appendQueryParameter("apiKey", apiKey)
            .appendQueryParameter("baseCurrencyCode", moonpayCurrency.currencyCode.uppercase())
            .appendQueryParameter("refundWalletAddress", walletAddress)
            .appendQueryParameter("redirectURL", "tangem://redirect_sell?currency_id=${cryptoCurrency.id.value}")

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
    val availableForSell: List<MoonPayAvailableCurrency>,
    val responseUserStatus: MoonPayUserStatus,
    val responseCurrencies: List<MoonPayCurrencies>,
)