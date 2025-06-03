package com.tangem.data.onramp

import android.net.Uri
import com.squareup.moshi.Moshi
import com.tangem.blockchain.extensions.toBigDecimalOrDefault
import com.tangem.data.common.api.safeApiCall
import com.tangem.data.onramp.converters.CountryConverter
import com.tangem.data.onramp.converters.CurrencyConverter
import com.tangem.data.onramp.converters.PaymentMethodConverter
import com.tangem.data.onramp.converters.StatusConverter
import com.tangem.data.onramp.converters.error.OnrampErrorConverter
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.express.TangemExpressApi
import com.tangem.datasource.api.express.models.TangemExpressValues
import com.tangem.datasource.api.express.models.response.ExchangeProvider
import com.tangem.datasource.api.express.models.response.ExchangeProviderType
import com.tangem.datasource.api.express.models.response.ExpressErrorResponse
import com.tangem.datasource.api.onramp.OnrampApi
import com.tangem.datasource.api.onramp.models.common.OnrampDestinationDTO
import com.tangem.datasource.api.onramp.models.request.OnrampPairsRequest
import com.tangem.datasource.api.onramp.models.response.OnrampDataJson
import com.tangem.datasource.api.onramp.models.response.model.OnrampCountryDTO
import com.tangem.datasource.api.onramp.models.response.model.OnrampPairDTO
import com.tangem.datasource.api.onramp.models.response.model.PaymentMethodDTO
import com.tangem.datasource.crypto.DataSignatureVerifier
import com.tangem.datasource.exchangeservice.swap.ExpressUtils
import com.tangem.datasource.local.onramp.countries.OnrampCountriesStore
import com.tangem.datasource.local.onramp.currencies.OnrampCurrenciesStore
import com.tangem.datasource.local.onramp.pairs.OnrampPairsStore
import com.tangem.datasource.local.onramp.paymentmethods.OnrampPaymentMethodsStore
import com.tangem.datasource.local.onramp.quotes.OnrampQuotesStore
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObject
import com.tangem.datasource.local.preferences.utils.getObjectSyncOrNull
import com.tangem.datasource.local.preferences.utils.storeObject
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.onramp.model.*
import com.tangem.domain.onramp.model.cache.OnrampTransaction
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.onramp.model.error.OnrampPairsError
import com.tangem.domain.onramp.model.error.OnrampRedirectError
import com.tangem.domain.onramp.repositories.OnrampRepository
import com.tangem.domain.tokens.model.Amount
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import org.joda.time.DateTime
import timber.log.Timber
import java.util.UUID

@Suppress("LongParameterList", "LargeClass", "TooManyFunctions")
internal class DefaultOnrampRepository(
    private val onrampApi: OnrampApi,
    private val expressApi: TangemExpressApi,
    private val dispatchers: CoroutineDispatcherProvider,
    private val appPreferencesStore: AppPreferencesStore,
    private val paymentMethodsStore: OnrampPaymentMethodsStore,
    private val pairsStore: OnrampPairsStore,
    private val quotesStore: OnrampQuotesStore,
    private val countriesStore: OnrampCountriesStore,
    private val currenciesStore: OnrampCurrenciesStore,
    private val walletManagersFacade: WalletManagersFacade,
    private val dataSignatureVerifier: DataSignatureVerifier,
    moshi: Moshi,
) : OnrampRepository {

    private val currencyConverter = CurrencyConverter()
    private val countryConverter = CountryConverter(currencyConverter)
    private val statusConverter = StatusConverter()
    private val paymentMethodsConverter = PaymentMethodConverter()
    private val onrampDataAdapter = moshi.adapter(OnrampDataJson::class.java)
    private val onrampErrorAdapter = moshi.adapter(ExpressErrorResponse::class.java)
    private val onrampErrorConverter = OnrampErrorConverter(onrampErrorAdapter)

    override fun getCurrencies(): Flow<List<OnrampCurrency>> = currenciesStore.get(CURRENCIES_KEY)

    override suspend fun fetchCurrencies(userWallet: UserWallet) = withContext(dispatchers.io) {
        if (!currenciesStore.getSyncOrNull(CURRENCIES_KEY).isNullOrEmpty()) return@withContext

        val result = onrampApi.getCurrencies(
            userWalletId = userWallet.walletId.stringValue,
            refCode = ExpressUtils.getRefCode(
                userWallet = userWallet,
                appPreferencesStore = appPreferencesStore,
            ),
        )
            .getOrThrow()
            .map(currencyConverter::convert)

        currenciesStore.store(CURRENCIES_KEY, result)
    }

    override fun getCountries(): Flow<List<OnrampCountry>> = countriesStore.get(COUNTRIES_KEY)

    override suspend fun getCountriesSync(): List<OnrampCountry>? {
        return countriesStore.getSyncOrNull(COUNTRIES_KEY)
    }

    override suspend fun fetchCountries(userWallet: UserWallet): List<OnrampCountry> = withContext(dispatchers.io) {
        if (!countriesStore.getSyncOrNull(COUNTRIES_KEY).isNullOrEmpty()) return@withContext emptyList()

        val result = onrampApi.getCountries(
            userWalletId = userWallet.walletId.stringValue,
            refCode = ExpressUtils.getRefCode(
                userWallet = userWallet,
                appPreferencesStore = appPreferencesStore,
            ),
        )
            .getOrThrow()
            .map(countryConverter::convert)

        countriesStore.store(COUNTRIES_KEY, result)

        result
    }

    override suspend fun getCountryByIp(userWallet: UserWallet): OnrampCountry = withContext(dispatchers.io) {
        onrampApi.getCountryByIp(
            userWalletId = userWallet.walletId.stringValue,
            refCode = ExpressUtils.getRefCode(
                userWallet = userWallet,
                appPreferencesStore = appPreferencesStore,
            ),
        )
            .getOrThrow()
            .let(countryConverter::convert)
    }

    override suspend fun getStatus(userWallet: UserWallet, txId: String): OnrampStatus = withContext(dispatchers.io) {
        onrampApi.getStatus(
            userWalletId = userWallet.walletId.stringValue,
            refCode = ExpressUtils.getRefCode(
                userWallet = userWallet,
                appPreferencesStore = appPreferencesStore,
            ),
            txId = txId,
        )
            .getOrThrow()
            .let(statusConverter::convert)
    }

    override suspend fun saveDefaultCurrency(currency: OnrampCurrency) = withContext(dispatchers.io) {
        val country = getDefaultCountrySync() ?: return@withContext
        appPreferencesStore.storeObject<OnrampCountryDTO>(
            key = PreferencesKeys.ONRAMP_DEFAULT_COUNTRY,
            value = countryConverter.convertBack(country.copy(defaultCurrency = currency)),
        )
    }

    override suspend fun getDefaultCurrencySync(): OnrampCurrency? = withContext(dispatchers.io) {
        appPreferencesStore
            .getObjectSyncOrNull<OnrampCountryDTO>(PreferencesKeys.ONRAMP_DEFAULT_COUNTRY)
            ?.let(countryConverter::convert)?.defaultCurrency
    }

    override fun getDefaultCurrency(): Flow<OnrampCurrency?> {
        return appPreferencesStore
            .getObject<OnrampCountryDTO>(PreferencesKeys.ONRAMP_DEFAULT_COUNTRY)
            .map { it?.let(countryConverter::convert)?.defaultCurrency }
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

    override suspend fun fetchPaymentMethodsIfAbsent(userWallet: UserWallet) = withContext(dispatchers.io) {
        if (paymentMethodsStore.contains(PAYMENT_METHODS_KEY)) return@withContext

        val response = safeApiCall(
            call = {
                onrampApi.getPaymentMethods(
                    userWalletId = userWallet.walletId.stringValue,
                    refCode = ExpressUtils.getRefCode(
                        userWallet = userWallet,
                        appPreferencesStore = appPreferencesStore,
                    ),
                ).bind()
            },
            onError = {
                Timber.w(it, "Unable to fetch onramp payment methods")
                throw it
            },
        )
        paymentMethodsStore.store(PAYMENT_METHODS_KEY, response.removeApplePay())
    }

    override suspend fun fetchPairs(
        userWallet: UserWallet,
        currency: OnrampCurrency,
        country: OnrampCountry,
        cryptoCurrency: CryptoCurrency,
    ) = withContext(dispatchers.io) {
        val onrampPairs = async {
            safeApiCall(
                call = {
                    onrampApi.getPairs(
                        userWalletId = userWallet.walletId.stringValue,
                        refCode = ExpressUtils.getRefCode(
                            userWallet = userWallet,
                            appPreferencesStore = appPreferencesStore,
                        ),
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
        }
        val providers = async {
            safeApiCall(
                call = {
                    expressApi.getProviders(
                        userWalletId = userWallet.walletId.stringValue,
                        refCode = ExpressUtils.getRefCode(
                            userWallet = userWallet,
                            appPreferencesStore = appPreferencesStore,
                        ),
                    ).bind()
                },
                onError = {
                    Timber.w(it, "Unable to fetch express providers")
                    throw it
                },
            )
        }
        storeOnrampPairs(pairs = onrampPairs.await(), providers = providers.await())
    }

    override suspend fun fetchQuotes(userWallet: UserWallet, cryptoCurrency: CryptoCurrency, amount: Amount) =
        withContext(dispatchers.io) {
            val pairs = requireNotNull(pairsStore.getSyncOrNull(PAIRS_KEY)) {
                "Unable to get pairs. At this point they must not be null."
            }
            val amountValue = requireNotNull(amount.value) { "Amount value must not be null" }
            val fromAmount = amountValue.movePointRight(amount.decimals).toString()
            val currency = requireNotNull(getDefaultCurrencySync()) { "Default currency must not be null" }
            val country = requireNotNull(getDefaultCountrySync()) { "Default country must not be null" }
            val fromOnrampAmount = OnrampAmount(
                value = fromAmount
                    .toBigDecimalOrDefault()
                    .movePointLeft(currency.precision),
                decimals = currency.precision,
                symbol = amount.currencySymbol,
            )
            val quotes: List<OnrampQuote> = pairs.flatMap { pair ->
                pair.providers.flatMap { provider ->
                    provider.paymentMethods.map { paymentMethod ->
                        async {
                            safeApiCall(
                                call = {
                                    val response = onrampApi.getQuote(
                                        fromCurrencyCode = currency.code,
                                        fromPrecision = currency.precision,
                                        toContractAddress = cryptoCurrency.getContractAddress(),
                                        toNetwork = cryptoCurrency.network.backendId,
                                        paymentMethod = paymentMethod.id,
                                        countryCode = country.code,
                                        fromAmount = fromAmount,
                                        toDecimals = cryptoCurrency.decimals,
                                        providerId = provider.id,
                                        userWalletId = userWallet.walletId.stringValue,
                                        refCode = ExpressUtils.getRefCode(
                                            userWallet = userWallet,
                                            appPreferencesStore = appPreferencesStore,
                                        ),
                                    ).bind()
                                    OnrampQuote.Data(
                                        fromAmount = fromOnrampAmount,
                                        toAmount = convertToAmount(response.toAmount, cryptoCurrency),
                                        minFromAmount = convertToAmount(response.minFromAmount, cryptoCurrency),
                                        maxFromAmount = convertToAmount(response.maxFromAmount, cryptoCurrency),
                                        paymentMethod = paymentMethod,
                                        provider = provider,
                                        countryCode = response.countryCode,
                                    )
                                },
                                onError = { error ->
                                    convertQuoteError(
                                        error = error,
                                        paymentMethod = paymentMethod,
                                        provider = provider,
                                        fromOnrampAmount = fromOnrampAmount,
                                        countryCode = country.code,
                                    )
                                },
                            )
                        }
                    }
                }
            }.awaitAll().filterNotNull()
            quotesStore.store(QUOTES_KEY, quotes)
        }

    override fun getQuotes(): Flow<List<OnrampQuote>> {
        return quotesStore.get(QUOTES_KEY)
    }

    override suspend fun getQuotesSync(): List<OnrampQuote>? {
        return quotesStore.getSyncOrNull(QUOTES_KEY)
    }

    override suspend fun getOnrampData(
        userWallet: UserWallet,
        cryptoCurrency: CryptoCurrency,
        quote: OnrampProviderWithQuote.Data,
        isDarkTheme: Boolean,
    ): OnrampTransaction = withContext(dispatchers.io) {
        try {
            val address = requireNotNull(
                value = walletManagersFacade.getDefaultAddress(userWallet.walletId, cryptoCurrency.network),
                lazyMessage = { "Address must not be null" },
            )
            val fromAmountString = quote.fromAmount.value.movePointRight(quote.fromAmount.decimals).toString()
            val currency = requireNotNull(getDefaultCurrencySync()) { "Default currency must not be null" }
            val country = requireNotNull(getDefaultCountrySync()) { "Default country must not be null" }
            val requestId = UUID.randomUUID().toString()
            val redirectUrl = quote.provider.toRedirectUrl() ?: error("Invalid onramp redirect url")
            val data = safeApiCall(
                call = {
                    onrampApi.getData(
                        fromCurrencyCode = currency.code,
                        fromPrecision = currency.precision,
                        toContractAddress = cryptoCurrency.getContractAddress(),
                        toNetwork = cryptoCurrency.network.backendId,
                        paymentMethod = quote.paymentMethod.id,
                        countryCode = country.code,
                        fromAmount = fromAmountString,
                        toDecimals = cryptoCurrency.decimals,
                        providerId = quote.provider.id,
                        toAddress = address,
                        redirectUrl = redirectUrl,
                        language = null,
                        theme = getTheme(isDarkTheme),
                        requestId = requestId,
                        userWalletId = userWallet.walletId.stringValue,
                        refCode = ExpressUtils.getRefCode(
                            userWallet = userWallet,
                            appPreferencesStore = appPreferencesStore,
                        ),
                    ).bind()
                },
                onError = { e ->
                    Timber.e(e)
                    throw e
                },
            )
            if (dataSignatureVerifier.verifySignature(data.signature, data.dataJson)) {
                val dataJson = requireNotNull(onrampDataAdapter.fromJson(data.dataJson)) {
                    "Can not parse dataJson. ${data.dataJson}"
                }
                if (requestId != dataJson.requestId) throw OnrampRedirectError.WrongRequestId

                createOnrampTransaction(
                    txId = data.txId,
                    quote = quote,
                    onrampDataJson = dataJson,
                    userWalletId = userWallet.walletId,
                    currency = currency,
                    cryptoCurrency = cryptoCurrency,
                    residency = country.name,
                )
            } else {
                throw OnrampRedirectError.VerificationFailed
            }
        } catch (e: Exception) {
            Timber.e(e)
            throw e
        }
    }

    override suspend fun getAvailablePaymentMethods(): Set<OnrampPaymentMethod> {
        val quotes = requireNotNull(quotesStore.getSyncOrNull(QUOTES_KEY)) { "Quotes must not be null" }
        return quotes.mapTo(hashSetOf(), OnrampQuote::paymentMethod)
    }

    override suspend fun clearCache() = withContext(NonCancellable) {
        paymentMethodsStore.clear()
        pairsStore.clear()
        quotesStore.clear()
        countriesStore.clear()
        currenciesStore.clear()
    }

    private suspend fun storeOnrampPairs(pairs: List<OnrampPairDTO>, providers: List<ExchangeProvider>) {
        if (pairs.isEmpty() || providers.isEmpty()) throw OnrampPairsError.PairsNotFound
        val onrampPaymentMethods = getPaymentMethods()
        val onrampPairs = pairs.map { pair ->
            val onrampProviders = pair.providers.mapNotNull { onrampProviderDTO ->
                val providerInfo = providers.find { provider ->
                    provider.id == onrampProviderDTO.providerId
                } ?: return@mapNotNull null

                OnrampProvider(
                    id = onrampProviderDTO.providerId,
                    info = OnrampProviderInfo(
                        name = providerInfo.name,
                        imageLarge = providerInfo.imageLargeUrl,
                        termsOfUseLink = providerInfo.termsOfUse,
                        privacyPolicyLink = providerInfo.privacyPolicy,
                    ),
                    paymentMethods = onrampPaymentMethods.filter { paymentMethod ->
                        onrampProviderDTO.paymentMethods.any { paymentMethod.id == it }
                    },
                )
            }

            if (onrampProviders.isEmpty()) throw OnrampPairsError.PairsNotFound

            OnrampPair(onrampProviders)
        }
        pairsStore.store(PAIRS_KEY, onrampPairs)
    }

    private suspend fun getPaymentMethods(): List<OnrampPaymentMethod> {
        return requireNotNull(paymentMethodsStore.getSyncOrNull(PAYMENT_METHODS_KEY)) {
            "Onramp payment methods is absent in storage"
        }.let(paymentMethodsConverter::convertList)
    }

    private fun createOnrampTransaction(
        txId: String,
        quote: OnrampProviderWithQuote.Data,
        onrampDataJson: OnrampDataJson,
        userWalletId: UserWalletId,
        currency: OnrampCurrency,
        cryptoCurrency: CryptoCurrency,
        residency: String,
    ): OnrampTransaction {
        return OnrampTransaction(
            txId = txId,
            userWalletId = userWalletId,
            fromAmount = quote.fromAmount.value,
            fromCurrency = currency,
            toAmount = quote.toAmount.value,
            toCurrencyId = cryptoCurrency.id.value,
            status = OnrampStatus.Status.Created,
            externalTxUrl = onrampDataJson.externalTxUrl,
            externalTxId = onrampDataJson.externalTxId,
            timestamp = DateTime.now().millis,
            providerName = quote.provider.info.name,
            providerImageUrl = quote.provider.info.imageLarge,
            providerType = ExchangeProviderType.ONRAMP.name,
            redirectUrl = onrampDataJson.widgetUrl,
            paymentMethod = quote.paymentMethod.name,
            residency = residency,
        )
    }

    private fun CryptoCurrency.getContractAddress(): String = when (this) {
        is CryptoCurrency.Coin -> TangemExpressValues.EMPTY_CONTRACT_ADDRESS_VALUE
        is CryptoCurrency.Token -> this.contractAddress
    }

    private fun convertToAmount(value: String, cryptoCurrency: CryptoCurrency): OnrampAmount {
        val cryptoValue = requireNotNull(value.toBigDecimalOrNull()) { "Can not parse $value to BigDecimal" }
        return OnrampAmount(
            value = cryptoValue.movePointLeft(cryptoCurrency.decimals),
            symbol = cryptoCurrency.symbol,
            decimals = cryptoCurrency.decimals,
        )
    }

    private fun getTheme(isDarkTheme: Boolean): String = if (isDarkTheme) {
        PROVIDER_THEME_DARK
    } else {
        PROVIDER_THEME_LIGHT
    }

    private fun List<PaymentMethodDTO>.removeApplePay(): List<PaymentMethodDTO> = filterNot { it.id == "apple-pay" }

    private fun convertQuoteError(
        error: Throwable,
        paymentMethod: OnrampPaymentMethod,
        provider: OnrampProvider,
        fromOnrampAmount: OnrampAmount,
        countryCode: String,
    ) = if (error is ApiResponseError.HttpException) {
        val onrampError = onrampErrorConverter.convert(value = error.errorBody.orEmpty())
        if (onrampError is OnrampError.AmountError) {
            OnrampQuote.AmountError(
                paymentMethod = paymentMethod,
                provider = provider,
                fromAmount = fromOnrampAmount,
                error = onrampError,
                countryCode = countryCode,
            )
        } else {
            Timber.w(error, "Unable to fetch onramp quotes for ${provider.id}. $error")
            OnrampQuote.Error(
                paymentMethod = paymentMethod,
                provider = provider,
                fromAmount = fromOnrampAmount,
                error = onrampError,
                countryCode = countryCode,
            )
        }
    } else {
        Timber.w(error, "Unable to fetch onramp quotes for ${provider.id}. $error")
        null
    }

    private fun OnrampProvider.toRedirectUrl() = Uri.Builder()
        .path(REDIRECT_URL)
        .appendPath(id)
        .build().path

    private companion object {
        const val PAYMENT_METHODS_KEY = "onramp_payment_methods"
        const val SELECTED_PAYMENT_METHOD_KEY = "onramp_selected_payment_method"
        const val PAIRS_KEY = "onramp_pairs"
        const val QUOTES_KEY = "onramp_quotes"
        const val COUNTRIES_KEY = "onramp_countries"
        const val CURRENCIES_KEY = "onramp_currencies"
        const val PROVIDER_THEME_DARK = "dark"
        const val PROVIDER_THEME_LIGHT = "light"

        const val REDIRECT_URL = "https://tangem.com/onramp"
    }
}