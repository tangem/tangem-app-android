package com.tangem.data.swap

import arrow.core.right
import com.squareup.moshi.Moshi
import com.tangem.data.common.api.safeApiCall
import com.tangem.data.swap.converter.SwapDataConverter
import com.tangem.data.swap.converter.SwapStatusConverter
import com.tangem.data.swap.converter.TokenInfoConverter
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.express.TangemExpressApi
import com.tangem.datasource.api.express.models.request.ExchangeSentRequestBody
import com.tangem.datasource.api.express.models.request.PairsRequestBody
import com.tangem.datasource.api.express.models.response.ExchangeDataResponseWithTxDetails
import com.tangem.datasource.api.express.models.response.TxDetails
import com.tangem.datasource.crypto.DataSignatureVerifier
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.datasource.exchangeservice.swap.ExpressUtils
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.domain.express.ExpressRepository
import com.tangem.domain.express.models.ExpressError
import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.express.models.ExpressProviderType
import com.tangem.domain.express.models.ExpressRateType
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.quotes.single.SingleQuoteStatusFetcher
import com.tangem.domain.quotes.single.SingleQuoteStatusProducer
import com.tangem.domain.quotes.single.SingleQuoteStatusSupplier
import com.tangem.domain.swap.SwapRepositoryV2
import com.tangem.domain.swap.models.*
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.utils.CurrencyStatusProxyCreator
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject

@Suppress("LongParameterList", "LargeClass")
internal class DefaultSwapRepositoryV2 @Inject constructor(
    private val tangemExpressApi: TangemExpressApi,
    private val expressRepository: ExpressRepository,
    private val coroutineDispatcher: CoroutineDispatcherProvider,
    private val appPreferencesStore: AppPreferencesStore,
    private val dataSignatureVerifier: DataSignatureVerifier,
    private val singleQuoteStatusSupplier: SingleQuoteStatusSupplier,
    private val singleQuoteStatusFetcher: SingleQuoteStatusFetcher,
    private val currencyStatusProxyCreator: CurrencyStatusProxyCreator,
    @NetworkMoshi moshi: Moshi,
) : SwapRepositoryV2 {

    private val swapDataConverter = SwapDataConverter()
    private val tokenInfoConverter = TokenInfoConverter()
    private val exchangeStatusConverter = SwapStatusConverter()
    private val txDetailsMoshiAdapter = moshi.adapter(TxDetails::class.java)

    override suspend fun getPairs(
        userWallet: UserWallet,
        initialCurrency: CryptoCurrency,
        cryptoCurrencyStatusList: List<CryptoCurrencyStatus>,
        filterProviderTypes: List<ExpressProviderType>,
    ): List<SwapPairModel> = withContext(coroutineDispatcher.io) {
        val cryptoCurrencyList = cryptoCurrencyStatusList.map { it.currency }

        val allPairs = getPairsInternal(
            userWallet = userWallet,
            initialCurrency = initialCurrency,
            cryptoCurrencyList = cryptoCurrencyList,
        )

        val providers = expressRepository.getProviders(
            userWallet = userWallet,
            filterProviderTypes = filterProviderTypes,
        )
        val mappedProviders = providers.associateBy(ExpressProvider::providerId)

        allPairs.map { pair ->
            async {
                val statusFrom = cryptoCurrencyStatusList
                    .firstOrNull {
                        it.currency.getContractAddress() == pair.from.contractAddress &&
                            it.currency.network.backendId == pair.from.network
                    }
                val statusTo = cryptoCurrencyStatusList
                    .firstOrNull {
                        it.currency.getContractAddress() == pair.to.contractAddress &&
                            it.currency.network.backendId == pair.to.network
                    }

                if (statusFrom != null && statusTo != null) {
                    SwapPairModel(
                        from = statusFrom,
                        to = statusTo,
                        providers = pair.providers.mapNotNull {
                            mappedProviders[it.providerId]
                        },
                    )
                } else {
                    null
                }
            }
        }.awaitAll().filterNotNull()
    }

    override suspend fun getSupportedPairs(
        userWallet: UserWallet,
        initialCurrency: CryptoCurrency,
        cryptoCurrencyList: List<CryptoCurrency>,
        filterProviderTypes: List<ExpressProviderType>,
    ): List<SwapPairModel> = withContext(coroutineDispatcher.io) {
        val allPairs = getPairsInternal(
            userWallet = userWallet,
            initialCurrency = initialCurrency,
            cryptoCurrencyList = cryptoCurrencyList,
        )

        val providers = expressRepository.getProviders(
            userWallet = userWallet,
            filterProviderTypes = filterProviderTypes,
        )
        val mappedProviders = providers.associateBy(ExpressProvider::providerId)

        allPairs.map { pair ->
            async {
                val statusFromDeferred = async {
                    cryptoCurrencyList
                        .firstOrNull {
                            it.getContractAddress() == pair.from.contractAddress &&
                                it.network.backendId == pair.from.network
                        }
                }
                val statusToDeferred = async {
                    cryptoCurrencyList
                        .firstOrNull {
                            it.getContractAddress() == pair.to.contractAddress &&
                                it.network.backendId == pair.to.network
                        }
                }

                val currencyStatusFrom = createSendWithSwapCryptoCurrencyStatus(statusFromDeferred.await())
                val currencyStatusTo = createSendWithSwapCryptoCurrencyStatus(statusToDeferred.await())

                if (currencyStatusFrom != null && currencyStatusTo != null) {
                    SwapPairModel(
                        from = currencyStatusFrom,
                        to = currencyStatusTo,
                        providers = pair.providers.mapNotNull {
                            mappedProviders[it.providerId]
                        },
                    )
                } else {
                    null
                }
            }
        }.awaitAll().filterNotNull()
    }

    override suspend fun getSwapQuote(
        userWallet: UserWallet,
        fromCryptoCurrency: CryptoCurrency,
        toCryptoCurrency: CryptoCurrency,
        fromAmount: BigDecimal,
        provider: ExpressProvider,
        rateType: ExpressRateType,
    ): SwapQuoteModel = withContext(coroutineDispatcher.io) {
        val response = tangemExpressApi.getExchangeQuote(
            fromAmount = fromAmount.movePointRight(fromCryptoCurrency.decimals).toString(),
            fromNetwork = fromCryptoCurrency.network.backendId,
            fromContractAddress = fromCryptoCurrency.getContractAddress(),
            fromDecimals = fromCryptoCurrency.decimals,
            toNetwork = toCryptoCurrency.network.backendId,
            toContractAddress = toCryptoCurrency.getContractAddress(),
            toDecimals = toCryptoCurrency.decimals,
            providerId = provider.providerId,
            rateType = rateType.name.lowercase(),
            userWalletId = userWallet.walletId.stringValue,
            refCode = ExpressUtils.getRefCode(
                userWallet = userWallet,
                appPreferencesStore = appPreferencesStore,
            ),
        ).getOrThrow()

        val toTokenAmount = requireNotNull(response.toAmount.toBigDecimalOrNull()?.movePointLeft(response.toDecimals))

        return@withContext SwapQuoteModel(
            provider = provider,
            toTokenAmount = toTokenAmount,
            allowanceContract = response.allowanceContract,
        )
    }

    override suspend fun getSwapData(
        userWallet: UserWallet,
        fromCryptoCurrencyStatus: CryptoCurrencyStatus,
        toCryptoCurrency: CryptoCurrency,
        fromAmount: String,
        toAddress: String,
        expressProvider: ExpressProvider,
        rateType: ExpressRateType,
    ): SwapDataModel = withContext(coroutineDispatcher.io) {
        val requestId = UUID.randomUUID().toString()
        val fromCryptoCurrency = fromCryptoCurrencyStatus.currency

        val refundData = when (expressProvider.type) {
            ExpressProviderType.CEX,
            ExpressProviderType.DEX_BRIDGE,
            ExpressProviderType.DEX,
            -> SwapRefundData(
                refundAddress = fromCryptoCurrencyStatus.value.networkAddress?.defaultAddress?.value,
                refundExtraId = null, // currently always null
            )
            else -> null
        }

        val response = tangemExpressApi.getExchangeData(
            fromContractAddress = fromCryptoCurrency.getContractAddress(),
            toContractAddress = toCryptoCurrency.getContractAddress(),
            fromNetwork = fromCryptoCurrency.network.backendId,
            toNetwork = toCryptoCurrency.network.backendId,
            fromAddress = fromCryptoCurrencyStatus.value.networkAddress?.defaultAddress?.value.orEmpty(),
            toAddress = toAddress,
            fromDecimals = fromCryptoCurrency.decimals,
            toDecimals = toCryptoCurrency.decimals,
            fromAmount = fromAmount,
            providerId = expressProvider.providerId,
            rateType = rateType.name.lowercase(),
            requestId = requestId,
            refundAddress = refundData?.refundAddress,
            refundExtraId = refundData?.refundExtraId,
            userWalletId = userWallet.walletId.stringValue,
            refCode = ExpressUtils.getRefCode(
                userWallet = userWallet,
                appPreferencesStore = appPreferencesStore,
            ),
        ).getOrThrow()

        if (dataSignatureVerifier.verifySignature(response.signature, response.txDetailsJson)) {
            val txDetails = parseTxDetails(response.txDetailsJson)
                ?: throw ExpressError.UnknownError
            if (txDetails.requestId != requestId) {
                throw ExpressError.InvalidRequestIdError()
            }
            if (!toAddress.equals(txDetails.payoutAddress, ignoreCase = true)) {
                throw ExpressError.InvalidPayoutAddressError()
            }
            swapDataConverter.convert(
                ExchangeDataResponseWithTxDetails(
                    dataResponse = response,
                    txDetails = txDetails,
                ),
            )
        } else {
            throw ExpressError.InvalidSignatureError()
        }
    }

    override suspend fun swapTransactionSent(
        userWallet: UserWallet,
        fromCryptoCurrencyStatus: CryptoCurrencyStatus,
        toAddress: String,
        txId: String,
        txHash: String,
        txExtraId: String?,
    ) {
        withContext(coroutineDispatcher.io) {
            tangemExpressApi.exchangeSent(
                userWalletId = userWallet.walletId.stringValue,
                refCode = ExpressUtils.getRefCode(
                    userWallet = userWallet,
                    appPreferencesStore = appPreferencesStore,
                ),
                body = ExchangeSentRequestBody(
                    txId = txId,
                    fromNetwork = fromCryptoCurrencyStatus.currency.network.backendId,
                    fromAddress = fromCryptoCurrencyStatus.value.networkAddress?.defaultAddress?.value.orEmpty(),
                    payinAddress = toAddress,
                    payinExtraId = txExtraId,
                    txHash = txHash,
                ),
            ).getOrThrow()
        }
    }

    override suspend fun getExchangeStatus(userWallet: UserWallet, txId: String): SwapStatusModel =
        withContext(coroutineDispatcher.io) {
            exchangeStatusConverter.convert(
                tangemExpressApi
                    .getExchangeStatus(
                        userWalletId = userWallet.walletId.stringValue,
                        refCode = ExpressUtils.getRefCode(
                            userWallet = userWallet,
                            appPreferencesStore = appPreferencesStore,
                        ),
                        txId = txId,
                    )
                    .getOrThrow(),
            )
        }

    private suspend fun CoroutineScope.getPairsInternal(
        userWallet: UserWallet,
        initialCurrency: CryptoCurrency,
        cryptoCurrencyList: List<CryptoCurrency>,
    ) = awaitAll(
        // original pairs
        async {
            invokePairRequest(
                userWallet = userWallet,
                from = arrayListOf(initialCurrency),
                to = cryptoCurrencyList,
            )
        },
        // reversed pairs
        async {
            invokePairRequest(
                userWallet = userWallet,
                from = cryptoCurrencyList,
                to = arrayListOf(initialCurrency),
            )
        },
    ).flatten()

    private suspend fun invokePairRequest(
        userWallet: UserWallet,
        from: List<CryptoCurrency>,
        to: List<CryptoCurrency>,
    ) = safeApiCall(
        call = {
            tangemExpressApi.getPairs(
                userWalletId = userWallet.walletId.stringValue,
                refCode = ExpressUtils.getRefCode(
                    userWallet = userWallet,
                    appPreferencesStore = appPreferencesStore,
                ),
                body = PairsRequestBody(
                    from = tokenInfoConverter.convertList(from),
                    to = tokenInfoConverter.convertList(to),
                ),
            ).getOrThrow()
        },
        onError = {
            Timber.w(it, "Unable to get pairs")
            throw it
        },
    )

    /**
     * Send with swap specific currency status creation
     * We support sending to any available to swap and supported network
     * It is possible that currency not added to wallet so we ignore network status
     */
    private suspend fun createSendWithSwapCryptoCurrencyStatus(cryptoCurrency: CryptoCurrency?): CryptoCurrencyStatus? {
        val rawCurrencyId = cryptoCurrency?.id?.rawCurrencyId ?: return null

        val quote = singleQuoteStatusSupplier.getSyncOrNull(
            params = SingleQuoteStatusProducer.Params(rawCurrencyId = rawCurrencyId),
        )?.right()

        if (quote == null) {
            singleQuoteStatusFetcher.invoke(
                params = SingleQuoteStatusFetcher.Params(
                    rawCurrencyId = rawCurrencyId,
                    appCurrencyId = null,
                ),
            )
        }

        return currencyStatusProxyCreator.createCurrencyStatus(
            currency = cryptoCurrency,
            maybeQuoteStatus = quote ?: singleQuoteStatusSupplier.getSyncOrNull(
                params = SingleQuoteStatusProducer.Params(rawCurrencyId = rawCurrencyId),
            ).right(),
            maybeNetworkStatus = NetworkStatus(
                network = cryptoCurrency.network,
                value = NetworkStatus.MissedDerivation, // Caution!!! Do not change this status
            ).right(),
            maybeYieldBalance = null,
        ).getOrNull()
    }

    private fun parseTxDetails(txDetailsJson: String): TxDetails? {
        return try {
            txDetailsMoshiAdapter.fromJson(txDetailsJson)
        } catch (e: IOException) {
            Timber.e(e, "error parsing txDetailsJson")
            null
        }
    }

    private fun CryptoCurrency.getContractAddress(): String {
        return when (this) {
            is CryptoCurrency.Token -> this.contractAddress
            is CryptoCurrency.Coin -> "0"
        }
    }
}