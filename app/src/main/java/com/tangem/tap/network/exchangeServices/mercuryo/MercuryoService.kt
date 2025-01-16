package com.tangem.tap.network.exchangeServices.mercuryo

import android.net.Uri
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.calculateSha512
import com.tangem.common.extensions.toHexString
import com.tangem.common.services.Result
import com.tangem.common.services.performRequest
import com.tangem.domain.core.utils.lceContent
import com.tangem.domain.core.utils.lceError
import com.tangem.domain.core.utils.lceLoading
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.tap.domain.model.Currency
import com.tangem.tap.network.exchangeServices.CurrencyExchangeManager
import com.tangem.tap.network.exchangeServices.ExchangeService
import com.tangem.tap.network.exchangeServices.ExchangeServiceInitializationStatus
import com.tangem.tap.network.exchangeServices.ExchangeUrlBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList

/**
[REDACTED_AUTHOR]
 */
internal class MercuryoService(private val environment: MercuryoEnvironment) : ExchangeService {

    override val initializationStatus: StateFlow<ExchangeServiceInitializationStatus>
        get() = _initializationStatus

    private val _initializationStatus: MutableStateFlow<ExchangeServiceInitializationStatus> =
        MutableStateFlow(value = lceLoading())

    private val api: MercuryoApi = environment.mercuryoApi

    private val availableMercuryoCurrencies = CopyOnWriteArrayList<MercuryoCurrenciesResponse.MercuryoCryptoCurrency>()

    override suspend fun update() {
        Timber.i("Start updating")
        _initializationStatus.value = lceLoading()

        val result = performRequest { api.currencies(environment.apiVersion) }
        when {
            result is Result.Success && result.data.status == RESPONSE_SUCCESS_STATUS_CODE -> {
                handleSuccessfullyUpdatedData(data = result.data.data)
            }
            result is Result.Failure -> {
                availableMercuryoCurrencies.clear()

                Timber.e("Failed to load currencies", result.error)
                _initializationStatus.value = result.error.lceError()
            }
        }
    }

    override fun availableForBuy(scanResponse: ScanResponse, currency: Currency): Boolean {
        val mercuryoNetwork = currency.blockchain.mercuryoNetwork
        val contractAddress = (currency as? Currency.Token)?.token?.contractAddress ?: ""
        val availableCurrency = availableMercuryoCurrencies.firstOrNull {
            it.currencySymbol == currency.currencySymbol &&
                it.network == mercuryoNetwork &&
                it.contractAddress.equals(contractAddress, ignoreCase = true)
        }
        return availableCurrency != null
    }

    override fun availableForSell(currency: Currency): Boolean = false

    override fun getUrl(
        action: CurrencyExchangeManager.Action,
        cryptoCurrency: CryptoCurrency,
        fiatCurrencyName: String,
        walletAddress: String,
        isDarkTheme: Boolean,
    ): String {
        if (action == CurrencyExchangeManager.Action.Sell) throw UnsupportedOperationException()

        val blockchain = Blockchain.fromId(cryptoCurrency.network.id.value)

        val builder = Uri.Builder()
            .scheme(ExchangeUrlBuilder.SCHEME)
            .authority("exchange.mercuryo.io")
            .appendQueryParameter("widget_id", environment.widgetId)
            .appendQueryParameter("type", action.name.lowercase())
            .appendQueryParameter("currency", cryptoCurrency.symbol)
            .appendQueryParameter("address", walletAddress)
            .appendQueryParameter("signature", signature(walletAddress))
            .appendQueryParameter("fix_currency", "true")
            .appendQueryParameter("redirect_url", ExchangeUrlBuilder.SUCCESS_URL)
        if (isDarkTheme) builder.appendQueryParameter("theme", "1inch")

        blockchain.mercuryoNetwork?.let {
            builder.appendQueryParameter("network", it)
        }

        return builder.build().toString()
    }

    override fun getSellCryptoReceiptUrl(action: CurrencyExchangeManager.Action, transactionId: String): String? = null

    private fun handleSuccessfullyUpdatedData(data: MercuryoCurrenciesResponse.Data) {
        availableMercuryoCurrencies.clear()
        availableMercuryoCurrencies.addAll(data.config.cryptoCurrencies)

        Timber.i("Successfully updated")
        _initializationStatus.value = lceContent()
    }

    private fun signature(address: String) = (address + environment.secret).calculateSha512().toHexString().lowercase()

    private companion object {
        const val RESPONSE_SUCCESS_STATUS_CODE = 200
    }
}