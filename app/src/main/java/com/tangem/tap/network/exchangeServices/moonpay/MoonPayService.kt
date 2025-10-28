package com.tangem.tap.network.exchangeServices.moonpay

import android.net.Uri
import android.util.Base64
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.common.services.Result
import com.tangem.common.services.performRequest
import com.tangem.datasource.api.moonpay.MoonPayApi
import com.tangem.datasource.api.moonpay.MoonPayCurrencies
import com.tangem.datasource.api.moonpay.MoonPayUserStatus
import com.tangem.domain.card.common.TapWorkarounds.isStart2Coin
import com.tangem.domain.common.extensions.withIOContext
import com.tangem.domain.core.utils.lceContent
import com.tangem.domain.core.utils.lceError
import com.tangem.domain.core.utils.lceLoading
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.tap.domain.model.Currency
import com.tangem.tap.network.exchangeServices.SellService
import com.tangem.tap.network.exchangeServices.SellServiceInitializationStatus
import com.tangem.tap.network.exchangeServices.moonpay.models.MoonPayAvailableCurrency
import com.tangem.utils.Provider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class MoonPayService(
    private val api: MoonPayApi,
    private val apiKeyProvider: Provider<String>,
    private val secretKeyProvider: Provider<String>,
    private val userWalletProvider: () -> UserWallet?,
) : SellService {

    override val initializationStatus: StateFlow<SellServiceInitializationStatus>
        get() = _initializationStatus

    private val _initializationStatus: MutableStateFlow<SellServiceInitializationStatus> =
        MutableStateFlow(value = lceLoading())

    private var status: MoonPayStatus? = null

    override suspend fun update() {
        withIOContext {
            Timber.i("Start updating")
            _initializationStatus.value = lceLoading()

            performRequest {
                val userStatus = when (val result = performRequest { api.getUserStatus(apiKeyProvider()) }) {
                    is Result.Failure -> {
                        Timber.e("Failed to load user status", result.error)
                        _initializationStatus.value = result.error.lceError()
                        return@performRequest
                    }
                    is Result.Success -> result.data
                }

                val currencies = when (val result = performRequest { api.getCurrencies(apiKeyProvider()) }) {
                    is Result.Failure -> {
                        Timber.e("Failed to load currencies", result.error)
                        _initializationStatus.value = result.error.lceError()
                        return@performRequest
                    }
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
                            contractAddress = currency.metadata?.contractAddress,
                        )
                    }

                Timber.i("Successfully updated")
                _initializationStatus.value = lceContent()
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

    override fun availableForSell(currency: Currency): Boolean {
        val userWallet = userWalletProvider() ?: return false
        val isExchangeSupported = userWallet !is UserWallet.Cold || !userWallet.scanResponse.card.isStart2Coin

        if (!isExchangeSupported) return false

        if (!isSellAllowed()) return false

        val availableForSell = status?.availableForSell ?: return false

        val supportedCurrency = currency.blockchain.moonPaySupportedCurrency ?: return false
        return availableForSell.any { availableCurrency ->
            when (currency) {
                is Currency.Blockchain -> {
                    availableCurrency.networkCode.equals(
                        other = supportedCurrency.networkCode,
                        ignoreCase = true,
                    ) && availableCurrency.currencyCode.equals(
                        other = supportedCurrency.currencyCode,
                        ignoreCase = true,
                    )
                }
                is Currency.Token -> {
                    availableCurrency.networkCode.equals(
                        other = supportedCurrency.networkCode,
                        ignoreCase = true,
                    ) &&
                        availableCurrency.contractAddress.equals(
                            other = currency.token.contractAddress,
                            ignoreCase = true,
                        )
                }
            }
        }
    }

    override fun getUrl(
        cryptoCurrency: CryptoCurrency,
        fiatCurrencyName: String,
        walletAddress: String,
        isDarkTheme: Boolean,
    ): String? {
        val blockchain = cryptoCurrency.network.toBlockchain()
        if (blockchain.isTestnet()) return blockchain.getTestnetTopUpUrl()

        val supportedCurrency = blockchain.moonPaySupportedCurrency ?: return null
        val moonpayCurrency = status?.availableForSell?.firstOrNull { availableCurrency ->
            when (cryptoCurrency) {
                is CryptoCurrency.Coin -> {
                    availableCurrency.networkCode.equals(other = supportedCurrency.networkCode, ignoreCase = true) &&
                        availableCurrency.currencyCode.equals(other = supportedCurrency.currencyCode, ignoreCase = true)
                }
                is CryptoCurrency.Token -> {
                    availableCurrency.networkCode.equals(other = supportedCurrency.networkCode, ignoreCase = true) &&
                        availableCurrency.contractAddress.equals(
                            other = cryptoCurrency.contractAddress,
                            ignoreCase = true,
                        )
                }
            }
        } ?: return null

        val uri = Uri.Builder()
            .scheme(SCHEME)
            .authority(URL_SELL)
            .appendQueryParameter("apiKey", apiKeyProvider())
            .appendQueryParameter("baseCurrencyCode", moonpayCurrency.currencyCode.uppercase())
            .appendQueryParameter("refundWalletAddress", walletAddress)
            .appendQueryParameter("redirectURL", "tangem://redirect_sell?currency_id=${cryptoCurrency.id.value}")

        if (isDarkTheme) uri.appendQueryParameter("theme", "dark")

        val originalQuery = uri.build().encodedQuery ?: uri.build().toString()
        val signature = createSignature(originalQuery)
        uri.appendQueryParameter("signature", signature)

        return uri.build().toString()
    }

    override fun getSellCryptoReceiptUrl(transactionId: String): String {
        return Uri.Builder()
            .scheme(SCHEME)
            .authority(URL_SELL)
            .appendPath("transaction_receipt")
            .appendQueryParameter("transactionId", transactionId).build().toString()
    }

    private fun createSignature(data: String): String {
        val sha256Hmac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(secretKeyProvider().toByteArray(), "HmacSHA256")
        sha256Hmac.init(secretKey)
        val sha256encoded = sha256Hmac.doFinal("?$data".toByteArray())
        return Base64.encodeToString(sha256encoded, Base64.NO_WRAP)
    }

    private fun isSellAllowed(): Boolean {
        return status?.responseUserStatus?.isSellAllowed == true
    }

    private companion object {
        const val URL_SELL = "sell.moonpay.com"
        const val SCHEME = "https"
    }
}

private data class MoonPayStatus(
    val availableForSell: List<MoonPayAvailableCurrency>,
    val responseUserStatus: MoonPayUserStatus,
    val responseCurrencies: List<MoonPayCurrencies>,
)