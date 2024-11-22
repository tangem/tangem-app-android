package com.tangem.data.onramp

import com.tangem.data.common.api.safeApiCall
import com.tangem.data.onramp.converters.CountryConverter
import com.tangem.data.onramp.converters.CurrencyConverter
import com.tangem.data.onramp.converters.PaymentMethodConverter
import com.tangem.data.onramp.converters.StatusConverter
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.express.models.TangemExpressValues
import com.tangem.datasource.api.onramp.OnrampApi
import com.tangem.datasource.api.onramp.models.common.OnrampDestinationDTO
import com.tangem.datasource.api.onramp.models.request.OnrampPairsRequest
import com.tangem.datasource.api.onramp.models.response.model.OnrampCountryDTO
import com.tangem.datasource.api.onramp.models.response.model.OnrampCurrencyDTO
import com.tangem.datasource.local.onramp.pairs.OnrampPairsStore
import com.tangem.datasource.local.onramp.paymentmethods.OnrampPaymentMethodsStore
import com.tangem.datasource.local.onramp.quotes.OnrampQuotesStore
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObject
import com.tangem.datasource.local.preferences.utils.getObjectSyncOrNull
import com.tangem.datasource.local.preferences.utils.storeObject
import com.tangem.domain.onramp.model.OnrampCountry
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.domain.onramp.model.OnrampPaymentMethod
import com.tangem.domain.onramp.model.OnrampStatus
import com.tangem.domain.onramp.repositories.OnrampRepository
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber

internal class DefaultOnrampRepository(
    private val onrampApi: OnrampApi,
    private val dispatchers: CoroutineDispatcherProvider,
    private val appPreferencesStore: AppPreferencesStore,
    private val paymentMethodsStore: OnrampPaymentMethodsStore,
    private val pairsStore: OnrampPairsStore,
    private val quotesStore: OnrampQuotesStore,
) : OnrampRepository {

    private val currencyConverter = CurrencyConverter()
    private val countryConverter = CountryConverter(currencyConverter)
    private val statusConverter = StatusConverter()
    private val paymentMethodsConverter = PaymentMethodConverter()

    override suspend fun getCurrencies(): List<OnrampCurrency> = withContext(dispatchers.io) {
        onrampApi.getCurrencies()
            .getOrThrow()
            .map(currencyConverter::convert)
    }

    override suspend fun getCountries(): List<OnrampCountry> = withContext(dispatchers.io) {
        onrampApi.getCountries()
            .getOrThrow()
            .map(countryConverter::convert)
    }

    override suspend fun getCountryByIp(): OnrampCountry = withContext(dispatchers.io) {
        onrampApi.getCountryByIp()
            .getOrThrow()
            .let(countryConverter::convert)
    }

    override suspend fun getStatus(txId: String): OnrampStatus = withContext(dispatchers.io) {
        onrampApi.getStatus(txId)
            .getOrThrow()
            .let(statusConverter::convert)
    }

    override suspend fun saveDefaultCurrency(currency: OnrampCurrency) = withContext(dispatchers.io) {
        appPreferencesStore.storeObject<OnrampCurrencyDTO>(
            key = PreferencesKeys.ONRAMP_DEFAULT_CURRENCY,
            value = currencyConverter.convertBack(currency),
        )
    }

    override suspend fun getDefaultCurrencySync(): OnrampCurrency? = withContext(dispatchers.io) {
        appPreferencesStore
            .getObjectSyncOrNull<OnrampCurrencyDTO>(PreferencesKeys.ONRAMP_DEFAULT_CURRENCY)
            ?.let(currencyConverter::convert)
    }

    override fun getDefaultCurrency(): Flow<OnrampCurrency?> {
        return appPreferencesStore
            .getObject<OnrampCurrencyDTO>(PreferencesKeys.ONRAMP_DEFAULT_CURRENCY)
            .map { it?.let(currencyConverter::convert) }
    }

    override suspend fun saveDefaultCountry(country: OnrampCountry) = withContext(dispatchers.io) {
        appPreferencesStore.storeObject<OnrampCountryDTO>(
            key = PreferencesKeys.ONRAMP_DEFAULT_COUNTRY,
            value = countryConverter.convertBack(country),
        )
    }

    override suspend fun getDefaultCountrySync(): OnrampCountry? = withContext(dispatchers.io) {
        appPreferencesStore
            .getObjectSyncOrNull<OnrampCountryDTO>(PreferencesKeys.ONRAMP_DEFAULT_COUNTRY)
            ?.let(countryConverter::convert)
    }

    override fun getDefaultCountry(): Flow<OnrampCountry?> {
        return appPreferencesStore
            .getObject<OnrampCountryDTO>(PreferencesKeys.ONRAMP_DEFAULT_COUNTRY)
            .map { it?.let(countryConverter::convert) }
    }

    override suspend fun fetchPaymentMethodsIfAbsent() = withContext(dispatchers.io) {
        if (paymentMethodsStore.contains(PAYMENT_METHODS_KEY)) return@withContext

        val response = safeApiCall(
            call = { onrampApi.getPaymentMethods().bind() },
            onError = {
                Timber.w(it, "Unable to fetch onramp payment methods")
                throw it
            },
        )
        paymentMethodsStore.store(PAYMENT_METHODS_KEY, response)
    }

    override suspend fun fetchPairs(currency: OnrampCurrency, country: OnrampCountry, cryptoCurrency: CryptoCurrency) =
        withContext(dispatchers.io) {
            val pairs = safeApiCall(
                call = {
                    onrampApi.getPairs(
                        body = OnrampPairsRequest(
                            fromCurrencyCode = currency.code,
                            countryCode = country.code,
                            to = listOf(
                                OnrampDestinationDTO(
                                    contractAddress = cryptoCurrency.getContractAddress(),
                                    network = cryptoCurrency.network.backendId,
                                ),
                            ),
                        ),
                    ).bind()
                },
                onError = {
                    Timber.w(it, "Unable to fetch onramp pairs")
                    throw it
                },
            )
            pairsStore.store(PAIRS_KEY, pairs)
        }

    override suspend fun fetchQuotes(
        currency: OnrampCurrency,
        country: OnrampCountry,
        cryptoCurrency: CryptoCurrency,
        fromAmount: String,
    ) = withContext(dispatchers.io) {
        val pairs = requireNotNull(pairsStore.getSyncOrNull(PAIRS_KEY)) {
            "Unable to get pairs. At this point they must not be null."
        }
        val quotes = pairs.flatMap { pair ->
            pair.providers.flatMap { provider ->
                provider.paymentMethods.map { paymentMethod ->
                    async {
                        safeApiCall(
                            call = {
                                onrampApi.getQuote(
                                    fromCurrencyCode = currency.code,
                                    toContractAddress = cryptoCurrency.getContractAddress(),
                                    toNetwork = cryptoCurrency.network.backendId,
                                    paymentMethod = paymentMethod,
                                    countryCode = country.code,
                                    fromAmount = fromAmount,
                                    toDecimals = cryptoCurrency.decimals,
                                    providerId = provider.providerId,
                                ).bind()
                            },
                            onError = {
                                Timber.w(
                                    it,
                                    "Unable to fetch onramp quotes for ${provider.providerId} -> $paymentMethod",
                                )
                                null
                            },
                        )
                    }
                }
            }
        }
            .awaitAll()
            .filterNotNull()
        quotesStore.store(QUOTES_KEY, quotes)
    }

    override suspend fun getPaymentMethods(): List<OnrampPaymentMethod> {
        val paymentMethods = requireNotNull(paymentMethodsStore.getSyncOrNull(PAYMENT_METHODS_KEY)) {
            "Onramp payment methods is absent in storage"
        }
        return paymentMethodsConverter.convertList(paymentMethods)
    }

    override suspend fun clearCache() {
        paymentMethodsStore.clear()
        pairsStore.clear()
        quotesStore.clear()
    }

    private fun CryptoCurrency.getContractAddress(): String = when (this) {
        is CryptoCurrency.Coin -> TangemExpressValues.EMPTY_CONTRACT_ADDRESS_VALUE
        is CryptoCurrency.Token -> this.contractAddress
    }

    private companion object {
        const val PAYMENT_METHODS_KEY = "onramp_payment_methods"
        const val PAIRS_KEY = "onramp_pairs"
        const val QUOTES_KEY = "onramp_quotes"
    }
}
