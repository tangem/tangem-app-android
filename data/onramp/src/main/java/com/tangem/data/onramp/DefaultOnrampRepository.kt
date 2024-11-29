package com.tangem.data.onramp

import com.squareup.moshi.Moshi
import com.tangem.blockchain.extensions.toBigDecimalOrDefault
import com.tangem.data.common.api.safeApiCall
import com.tangem.data.onramp.converters.CountryConverter
import com.tangem.data.onramp.converters.CurrencyConverter
import com.tangem.data.onramp.converters.PaymentMethodConverter
import com.tangem.data.onramp.converters.StatusConverter
import com.tangem.data.onramp.converters.error.OnrampQuoteErrorInput
import com.tangem.data.onramp.converters.error.OnrampQuotesErrorConverter
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.express.TangemExpressApi
import com.tangem.datasource.api.express.models.TangemExpressValues
import com.tangem.datasource.api.express.models.response.ExchangeProvider
import com.tangem.datasource.api.express.models.response.ExchangeProviderType
import com.tangem.datasource.api.onramp.OnrampApi
import com.tangem.datasource.api.onramp.models.common.OnrampDestinationDTO
import com.tangem.datasource.api.onramp.models.request.OnrampPairsRequest
import com.tangem.datasource.api.onramp.models.response.OnrampDataJson
import com.tangem.datasource.api.onramp.models.response.model.OnrampCountryDTO
import com.tangem.datasource.api.onramp.models.response.model.OnrampCurrencyDTO
import com.tangem.datasource.api.onramp.models.response.model.OnrampPairDTO
import com.tangem.datasource.api.onramp.models.response.model.PaymentMethodDTO
import com.tangem.datasource.crypto.DataSignatureVerifier
import com.tangem.datasource.local.onramp.pairs.OnrampPairsStore
import com.tangem.datasource.local.onramp.paymentmethods.OnrampPaymentMethodsStore
import com.tangem.datasource.local.onramp.quotes.OnrampQuotesStore
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObject
import com.tangem.datasource.local.preferences.utils.getObjectSyncOrNull
import com.tangem.datasource.local.preferences.utils.storeObject
import com.tangem.domain.onramp.model.*
import com.tangem.domain.onramp.model.cache.OnrampTransaction
import com.tangem.domain.onramp.repositories.OnrampRepository
import com.tangem.domain.tokens.model.Amount
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.walletmanager.WalletManagersFacade
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
import java.math.BigDecimal
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
    private val quotesErrorConverter: OnrampQuotesErrorConverter,
    private val walletManagersFacade: WalletManagersFacade,
    private val dataSignatureVerifier: DataSignatureVerifier,
    moshi: Moshi,
) : OnrampRepository {

    private val currencyConverter = CurrencyConverter()
    private val countryConverter = CountryConverter(currencyConverter)
    private val statusConverter = StatusConverter()
    private val paymentMethodsConverter = PaymentMethodConverter()
    private val onrampDataAdapter = moshi.adapter(OnrampDataJson::class.java)

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
        paymentMethodsStore.store(PAYMENT_METHODS_KEY, response.removeApplePay())
    }

    override suspend fun fetchPairs(currency: OnrampCurrency, country: OnrampCountry, cryptoCurrency: CryptoCurrency) =
        withContext(dispatchers.io) {
            val onrampPairs = async {
                safeApiCall(
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
            }
            val providers = async {
                safeApiCall(
                    call = { expressApi.getProviders().bind() },
                    onError = {
                        Timber.w(it, "Unable to fetch express providers")
                        throw it
                    },
                )
            }
            storeOnrampPairs(pairs = onrampPairs.await(), providers = providers.await())
        }

    override suspend fun fetchQuotes(cryptoCurrency: CryptoCurrency, amount: Amount) = withContext(dispatchers.io) {
        val pairs = requireNotNull(pairsStore.getSyncOrNull(PAIRS_KEY)) {
            "Unable to get pairs. At this point they must not be null."
        }
        val amountValue = requireNotNull(amount.value) { "Amount value must not be null" }
        val fromAmount = amountValue.movePointRight(amount.decimals).toString()
        val currency = requireNotNull(getDefaultCurrencySync()) { "Default currency must not be null" }
        val country = requireNotNull(getDefaultCountrySync()) { "Default country must not be null" }
        val quotes: List<OnrampQuote> = pairs.flatMap { pair ->
            pair.providers.flatMap { provider ->
                provider.paymentMethods.map { paymentMethod ->
                    async {
                        safeApiCall(
                            call = {
                                val response = onrampApi.getQuote(
                                    fromCurrencyCode = currency.code,
                                    toContractAddress = cryptoCurrency.getContractAddress(),
                                    toNetwork = cryptoCurrency.network.backendId,
                                    paymentMethod = paymentMethod.id,
                                    countryCode = country.code,
                                    fromAmount = fromAmount,
                                    toDecimals = cryptoCurrency.decimals,
                                    providerId = provider.id,
                                ).bind()
                                OnrampQuote.Data(
                                    fromAmount = OnrampAmount(
                                        value = response.fromAmount
                                            .toBigDecimalOrDefault()
                                            .movePointLeft(currency.precision),
                                        decimals = currency.precision,
                                        symbol = amount.currencySymbol,
                                    ),
                                    toAmount = convertToAmount(response.toAmount, cryptoCurrency),
                                    minFromAmount = convertToAmount(response.minFromAmount, cryptoCurrency),
                                    maxFromAmount = convertToAmount(response.maxFromAmount, cryptoCurrency),
                                    paymentMethod = paymentMethod,
                                    provider = provider,
                                )
                            },
                            onError = { error ->
                                if (error is ApiResponseError.HttpException) {
                                    quotesErrorConverter.convert(
                                        OnrampQuoteErrorInput(
                                            errorBody = error.errorBody.orEmpty(),
                                            amount = amount,
                                            paymentMethod = paymentMethod,
                                            provider = provider,
                                        ),
                                    )
                                } else {
                                    Timber.w(error, "Unable to fetch onramp quotes for ${provider.id}. $error")
                                    null
                                }
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
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
        quote: OnrampProviderWithQuote.Data,
    ): OnrampTransaction = withContext(dispatchers.io) {
        try {
            val address = requireNotNull(
                value = walletManagersFacade.getDefaultAddress(userWalletId, cryptoCurrency.network),
                lazyMessage = { "Address must not be null" },
            )
            val fromAmountString = quote.fromAmount.value.movePointRight(quote.fromAmount.decimals).toString()
            val currency = requireNotNull(getDefaultCurrencySync()) { "Default currency must not be null" }
            val country = requireNotNull(getDefaultCountrySync()) { "Default country must not be null" }
            val data = safeApiCall(
                call = {
                    onrampApi.getData(
                        fromCurrencyCode = currency.code,
                        toContractAddress = cryptoCurrency.getContractAddress(),
                        toNetwork = cryptoCurrency.network.backendId,
                        paymentMethod = quote.paymentMethod.id,
                        countryCode = country.code,
                        fromAmount = fromAmountString,
                        toDecimals = cryptoCurrency.decimals,
                        providerId = quote.provider.id,
                        toAddress = address,
                        redirectUrl = "tangem://redirect?action=dismissBrowser",
                        language = null,
                        theme = null,
                        requestId = UUID.randomUUID().toString(),
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
                createOnrampTransaction(
                    txId = data.txId,
                    onrampDataJson = dataJson,
                    userWalletId = userWalletId,
                    currency = currency,
                    provider = quote.provider,
                    cryptoCurrency = cryptoCurrency,
                    fromAmount = quote.fromAmount.value,
                    toAmount = quote.toAmount.value,
                )
            } else {
                error("Express signature invalid")
            }
        } catch (e: Exception) {
            Timber.e(e)
            throw e
        }
    }

    override suspend fun getPaymentMethods(): List<OnrampPaymentMethod> {
        val paymentMethods = requireNotNull(paymentMethodsStore.getSyncOrNull(PAYMENT_METHODS_KEY)) {
            "Onramp payment methods is absent in storage"
        }
        return paymentMethodsConverter.convertList(paymentMethods)
    }

    override suspend fun saveSelectedPaymentMethod(paymentMethod: OnrampPaymentMethod) {
        val dto = paymentMethodsConverter.convertBack(paymentMethod)
        paymentMethodsStore.store(SELECTED_PAYMENT_METHOD_KEY, listOf(dto))
    }

    override fun getSelectedPaymentMethod(): Flow<OnrampPaymentMethod> {
        return paymentMethodsStore
            .get(SELECTED_PAYMENT_METHOD_KEY)
            .mapNotNull { paymentMethods -> paymentMethods.firstOrNull()?.let(paymentMethodsConverter::convert) }
    }

    override suspend fun clearCache() = withContext(NonCancellable) {
        paymentMethodsStore.clear()
        pairsStore.clear()
        quotesStore.clear()
    }

    private suspend fun storeOnrampPairs(pairs: List<OnrampPairDTO>, providers: List<ExchangeProvider>) {
        val onrampPaymentMethods = getPaymentMethods()
        val onrampPairs = pairs.map { pair ->
            val onrampProviders = pair.providers.mapNotNull { onrampProviderDTO ->
                val providerInfo = providers.find { provider ->
                    provider.id == onrampProviderDTO.providerId
                } ?: return@mapNotNull null

                OnrampProvider(
                    id = onrampProviderDTO.providerId,
                    info = OnrampProviderInfo(name = providerInfo.name, imageLarge = providerInfo.imageLargeUrl),
                    paymentMethods = onrampPaymentMethods.filter { paymentMethod ->
                        onrampProviderDTO.paymentMethods.any { paymentMethod.id == it }
                    },
                )
            }
            OnrampPair(onrampProviders)
        }
        pairsStore.store(PAIRS_KEY, onrampPairs)
    }

    private fun createOnrampTransaction(
        txId: String,
        onrampDataJson: OnrampDataJson,
        userWalletId: UserWalletId,
        currency: OnrampCurrency,
        provider: OnrampProvider,
        cryptoCurrency: CryptoCurrency,
        fromAmount: BigDecimal,
        toAmount: BigDecimal,
    ): OnrampTransaction {
        return OnrampTransaction(
            txId = txId,
            userWalletId = userWalletId,
            fromAmount = fromAmount,
            fromCurrency = currency,
            toAmount = toAmount,
            toCurrencyId = cryptoCurrency.id.value,
            status = OnrampStatus.Status.Expired,
            externalTxUrl = onrampDataJson.externalTxUrl,
            externalTxId = onrampDataJson.externalTxId,
            timestamp = DateTime.now().millis,
            providerName = provider.info.name,
            providerImageUrl = provider.info.imageLarge,
            providerType = ExchangeProviderType.ONRAMP.name,
            redirectUrl = onrampDataJson.widgetUrl,
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

    private fun List<PaymentMethodDTO>.removeApplePay(): List<PaymentMethodDTO> = filterNot { it.id == "apple-pay" }

    private companion object {
        const val PAYMENT_METHODS_KEY = "onramp_payment_methods"
        const val SELECTED_PAYMENT_METHOD_KEY = "onramp_selected_payment_method"
        const val PAIRS_KEY = "onramp_pairs"
        const val QUOTES_KEY = "onramp_quotes"
    }
}