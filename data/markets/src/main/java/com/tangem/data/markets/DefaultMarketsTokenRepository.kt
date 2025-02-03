package com.tangem.data.markets

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.compatibility.applyL2Compatibility
import com.tangem.blockchainsdk.compatibility.getTokenIdIfL2Network
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.data.common.cache.CacheRegistry
import com.tangem.data.common.currency.CryptoCurrencyFactory
import com.tangem.data.common.currency.getNetwork
import com.tangem.data.common.utils.retryOnError
import com.tangem.data.markets.analytics.MarketsDataAnalyticsEvent
import com.tangem.data.markets.converters.*
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.markets.TangemTechMarketsApi
import com.tangem.datasource.api.markets.models.response.TokenMarketExchangesResponse
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.TangemTechApi.Companion.marketsQuoteFields
import com.tangem.datasource.local.datastore.RuntimeStateStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.markets.*
import com.tangem.domain.markets.repositories.MarketsTokenRepository
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.pagination.*
import com.tangem.pagination.fetcher.LimitOffsetBatchFetcher
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

@Suppress("LongParameterList")
internal class DefaultMarketsTokenRepository(
    private val marketsApi: TangemTechMarketsApi,
    private val tangemTechApi: TangemTechApi,
    private val userWalletsStore: UserWalletsStore,
    private val dispatcherProvider: CoroutineDispatcherProvider,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val excludedBlockchains: ExcludedBlockchains,
    private val cacheRegistry: CacheRegistry,
    private val tokenExchangesStore: RuntimeStateStore<List<TokenMarketExchangesResponse.Exchange>>,
) : MarketsTokenRepository {

    private val tokenMarketInfoConverter: TokenMarketInfoConverter = TokenMarketInfoConverter(excludedBlockchains)
    private val cryptoCurrencyFactory = CryptoCurrencyFactory(excludedBlockchains)

    private fun createTokenMarketsFetcher(firstBatchSize: Int, nextBatchSize: Int) = LimitOffsetBatchFetcher(
        prefetchDistance = firstBatchSize,
        batchSize = nextBatchSize,
        subFetcher = object : LimitOffsetBatchFetcher.SubFetcher<TokenMarketListConfig, List<TokenMarket>> {

            val requestTimeStamp = AtomicLong(0)

            override suspend fun fetch(
                request: LimitOffsetBatchFetcher.Request<TokenMarketListConfig>,
                lastResult: BatchFetchResult<List<TokenMarket>>?,
                isFirstBatchFetching: Boolean,
            ): BatchFetchResult<List<TokenMarket>> {
                val searchText =
                    if (request.params.searchText.isNullOrBlank()) null else request.params.searchText

                val requestCall = suspend {
                    marketsApi.getCoinsList(
                        currency = request.params.fiatPriceCurrency,
                        interval = request.params.priceChangeInterval.toRequestParam(),
                        order = request.params.order.toRequestParam(),
                        search = searchText,
                        offset = request.offset,
                        limit = request.limit,
                        timestamp = if (isFirstBatchFetching) null else requestTimeStamp.get(),
                    ).getOrThrow()
                }

                // we shouldn't infinitely retry on the first batch request
                val res = if (isFirstBatchFetching) {
                    catchListErrorAndSendEvent { requestCall() }
                } else {
                    retryOnError(priority = true) {
                        catchListErrorAndSendEvent { requestCall() }
                    }
                }

                if (isFirstBatchFetching) {
                    requestTimeStamp.set(res.timestamp ?: 0)
                }

                val last = res.tokens.size < request.limit

                return BatchFetchResult.Success(
                    data = TokenMarketListConverter.convert(res),
                    last = last,
                    empty = res.tokens.isEmpty(),
                )
            }
        },
    )

    override fun getTokenListFlow(
        batchingContext: BatchingContext<Int, TokenMarketListConfig, TokenMarketUpdateRequest>,
        firstBatchSize: Int,
        nextBatchSize: Int,
    ): BatchFlow<Int, List<TokenMarket>, TokenMarketUpdateRequest> {
        val tokenMarketsUpdateFetcher = MarketsBatchUpdateFetcher(
            tangemTechApi = tangemTechApi,
            marketsApi = marketsApi,
            analyticsEventHandler = analyticsEventHandler,
            onApiError = {
                analyticsEventHandler.send(createListErrorEvent(it).toEvent())
            },
        )

        val atomicInteger = AtomicInteger(0)

        return BatchListSource(
            fetchDispatcher = dispatcherProvider.io,
            context = batchingContext,
            generateNewKey = { atomicInteger.getAndIncrement() },
            batchFetcher = createTokenMarketsFetcher(firstBatchSize = firstBatchSize, nextBatchSize = nextBatchSize),
            updateFetcher = tokenMarketsUpdateFetcher,
        ).toBatchFlow()
    }

    override suspend fun getChart(
        fiatCurrencyCode: String,
        interval: PriceChangeInterval,
        tokenId: CryptoCurrency.RawID,
        tokenSymbol: String,
    ) = withContext(dispatcherProvider.io) {
        val mappedTokenId = getTokenIdIfL2Network(tokenId.value)
        val response = marketsApi.getCoinChart(
            currency = fiatCurrencyCode,
            coinId = mappedTokenId,
            interval = interval.toRequestParam(),
        )

        val result = catchDetailsErrorAndSendEvent(
            request = MarketsDataAnalyticsEvent.Details.Error.Request.Chart,
            tokenSymbol = tokenSymbol,
        ) {
            response.getOrThrow()
        }

        return@withContext TokenChartConverter.convert(
            interval = interval,
            value = result,

            // === Analytics ===
            onNullPresented = {
                analyticsEventHandler.send(
                    MarketsDataAnalyticsEvent.ChartNullValuesError(
                        requestPath = "coins/history",
                        errorType = MarketsDataAnalyticsEvent.Type.Custom,
                    ),
                )
            },
        )
    }

    override suspend fun getChartPreview(
        fiatCurrencyCode: String,
        interval: PriceChangeInterval,
        tokenId: CryptoCurrency.RawID,
        tokenSymbol: String,
    ) = withContext(dispatcherProvider.io) {
        val mappedTokenId = getTokenIdIfL2Network(tokenId.value)

        val chart = catchListErrorAndSendEvent {
            marketsApi.getCoinsListCharts(
                coinIds = mappedTokenId,
                currency = fiatCurrencyCode,
                interval = interval.toRequestParam(),
            ).getOrThrow()[mappedTokenId] ?: error(
                "No chart preview data for the token $mappedTokenId",
            )
        }
        return@withContext TokenChartConverter.convert(
            interval = interval,
            value = chart,

            // === Analytics ===
            onNullPresented = {
                analyticsEventHandler.send(
                    MarketsDataAnalyticsEvent.ChartNullValuesError(
                        requestPath = "coins/history_preview",
                        errorType = MarketsDataAnalyticsEvent.Type.Custom,
                    ),
                )
            },
        )
    }

    override suspend fun getTokenInfo(
        fiatCurrencyCode: String,
        tokenId: CryptoCurrency.RawID,
        tokenSymbol: String,
        languageCode: String,
    ) = withContext(dispatcherProvider.io) {
        val result = catchDetailsErrorAndSendEvent(
            request = MarketsDataAnalyticsEvent.Details.Error.Request.Info,
            tokenSymbol = tokenSymbol,
        ) {
            marketsApi.getCoinMarketData(
                currency = fiatCurrencyCode,
                coinId = tokenId.value,
                language = languageCode,
            ).getOrThrow()
        }

        val resultResponse = result.applyL2Compatibility(tokenId.value)
        return@withContext tokenMarketInfoConverter.convert(resultResponse)
    }

    override suspend fun getTokenQuotes(fiatCurrencyCode: String, tokenId: CryptoCurrency.RawID, tokenSymbol: String) =
        withContext(dispatcherProvider.io) {
            // for second markets iteration we should use extended api method with all required fields

            val result = catchDetailsErrorAndSendEvent(
                request = MarketsDataAnalyticsEvent.Details.Error.Request.Info,
                tokenSymbol = tokenSymbol,
            ) {
                tangemTechApi.getQuotes(
                    currencyId = fiatCurrencyCode,
                    coinIds = tokenId.value,
                    fields = marketsQuoteFields.joinToString(separator = ","),
                ).getOrThrow()
            }

            return@withContext TokenQuotesShortConverter.convert(tokenId, result).toFull()
        }

    override suspend fun createCryptoCurrency(
        userWalletId: UserWalletId,
        token: TokenMarketParams,
        network: TokenMarketInfo.Network,
    ): CryptoCurrency? {
        val userWallet = userWalletsStore.getSyncOrNull(userWalletId) ?: error("UserWalletId [$userWalletId] not found")
        val blockchain = Blockchain.fromNetworkId(network.networkId) ?: error("Unknown network [${network.networkId}]")

        return if (network.contractAddress == null) {
            cryptoCurrencyFactory.createCoin(
                blockchain = blockchain,
                extraDerivationPath = null,
                scanResponse = userWallet.scanResponse,
            )
        } else {
            val currencyNetwork = getNetwork(
                blockchain = blockchain,
                extraDerivationPath = null,
                scanResponse = userWallet.scanResponse,
                excludedBlockchains = excludedBlockchains,
            ) ?: return null

            cryptoCurrencyFactory.createToken(
                network = currencyNetwork,
                rawId = token.id,
                name = token.name,
                symbol = token.symbol,
                decimals = network.decimalCount ?: error("Unknown decimal"),
                contractAddress = network.contractAddress!!,
            )
        }
    }

    override suspend fun getTokenExchanges(tokenId: CryptoCurrency.RawID): List<TokenMarketExchange> {
        return withContext(dispatcherProvider.io) {
            cacheRegistry.invokeOnExpire(key = "coins/$tokenId/exchanges", skipCache = false) {
                val response = marketsApi.getCoinExchanges(coinId = tokenId.value).getOrThrow()

                tokenExchangesStore.store(value = response.exchanges)
            }

            TokenMarketExchangeConverter.convertList(input = tokenExchangesStore.get().value)
        }
    }

    inline fun <T> catchListErrorAndSendEvent(block: () -> T): T {
        return catchErrorAndSendEvent(block, ::createListErrorEvent)
    }

    private inline fun <T> catchDetailsErrorAndSendEvent(
        request: MarketsDataAnalyticsEvent.Details.Error.Request,
        tokenSymbol: String,
        block: () -> T,
    ): T {
        return catchErrorAndSendEvent(block) { error ->
            createDetailsErrorEvent(error, request, tokenSymbol)
        }
    }

    private inline fun <T> catchErrorAndSendEvent(
        block: () -> T,
        createErrorEvent: (ApiResponseError) -> MarketsDataAnalyticsEvent,
    ): T {
        return try {
            block()
        } catch (e: ApiResponseError) {
            val errorEvent = createErrorEvent(e)

            analyticsEventHandler.send(errorEvent.toEvent())
            throw e
        }
    }

    private fun createListErrorEvent(error: ApiResponseError): MarketsDataAnalyticsEvent.List.Error {
        return createErrorEvent(error) { errorType, errorCode, errorMessage ->
            MarketsDataAnalyticsEvent.List.Error(
                errorType = errorType,
                errorCode = errorCode,
                errorMessage = errorMessage,
            )
        }
    }

    private fun createDetailsErrorEvent(
        error: ApiResponseError,
        request: MarketsDataAnalyticsEvent.Details.Error.Request,
        tokenSymbol: String,
    ): MarketsDataAnalyticsEvent.Details.Error {
        return createErrorEvent(error) { errorType, errorCode, errorMessage ->
            MarketsDataAnalyticsEvent.Details.Error(
                errorType = errorType,
                errorCode = errorCode,
                errorMessage = errorMessage,
                request = request,
                tokenSymbol = tokenSymbol,
            )
        }
    }

    private inline fun <T> createErrorEvent(
        error: ApiResponseError,
        createEvent: (MarketsDataAnalyticsEvent.Type, Int?, String) -> T,
    ): T {
        return when (error) {
            is ApiResponseError.HttpException -> {
                createEvent(MarketsDataAnalyticsEvent.Type.Http, error.code.code, error.message.orEmpty())
            }
            is ApiResponseError.TimeoutException -> {
                createEvent(MarketsDataAnalyticsEvent.Type.Timeout, null, error.message.orEmpty())
            }
            is ApiResponseError.NetworkException -> {
                createEvent(MarketsDataAnalyticsEvent.Type.Network, null, error.message.orEmpty())
            }
            is ApiResponseError.UnknownException -> {
                createEvent(MarketsDataAnalyticsEvent.Type.Unknown, null, error.message.orEmpty())
            }
        }
    }
}