package com.tangem.data.markets

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.compatibility.applyL2Compatibility
import com.tangem.blockchainsdk.compatibility.getTokenIdIfL2Network
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.data.common.currency.CryptoCurrencyFactory
import com.tangem.data.common.currency.getNetwork
import com.tangem.data.common.utils.retryOnError
import com.tangem.data.markets.analytics.MarketsDataAnalyticsEvent
import com.tangem.data.markets.converters.*
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.markets.TangemTechMarketsApi
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.TangemTechApi.Companion.marketsQuoteFields
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.common.util.derivationStyleProvider
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

internal class DefaultMarketsTokenRepository(
    private val marketsApi: TangemTechMarketsApi,
    private val tangemTechApi: TangemTechApi,
    private val userWalletsStore: UserWalletsStore,
    private val dispatcherProvider: CoroutineDispatcherProvider,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : MarketsTokenRepository {

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
                    catchApiErrorAndSendEvent(errorEvent = MarketsDataAnalyticsEvent.List.Error) {
                        requestCall()
                    }
                } else {
                    retryOnError(priority = true) {
                        catchApiErrorAndSendEvent(errorEvent = MarketsDataAnalyticsEvent.List.Error) {
                            requestCall()
                        }
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
                analyticsEventHandler.send(MarketsDataAnalyticsEvent.List.Error.toEvent())
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
        tokenId: String,
        tokenSymbol: String,
    ) = withContext(dispatcherProvider.io) {
        val mappedTokenId = getTokenIdIfL2Network(tokenId)
        val response = marketsApi.getCoinChart(
            currency = fiatCurrencyCode,
            coinId = mappedTokenId,
            interval = interval.toRequestParam(),
        )

        val result = catchApiErrorAndSendEvent(
            errorEvent = MarketsDataAnalyticsEvent.Details.Error(
                request = MarketsDataAnalyticsEvent.Details.Error.Request.Chart,
                tokenSymbol = tokenSymbol,
            ),
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
                    ),
                )
            },
        )
    }

    override suspend fun getChartPreview(
        fiatCurrencyCode: String,
        interval: PriceChangeInterval,
        tokenId: String,
        tokenSymbol: String,
    ) = withContext(dispatcherProvider.io) {
        val mappedTokenId = getTokenIdIfL2Network(tokenId)
        val response = marketsApi.getCoinsListCharts(
            coinIds = mappedTokenId,
            currency = fiatCurrencyCode,
            interval = interval.toRequestParam(),
        )

        val chart = catchApiErrorAndSendEvent(errorEvent = MarketsDataAnalyticsEvent.List.Error) {
            response.getOrThrow()[mappedTokenId] ?: error(
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
                    ),
                )
            },
        )
    }

    override suspend fun getTokenInfo(
        fiatCurrencyCode: String,
        tokenId: String,
        tokenSymbol: String,
        languageCode: String,
    ) = withContext(dispatcherProvider.io) {
        val response = marketsApi.getCoinMarketData(
            currency = fiatCurrencyCode,
            coinId = tokenId,
            language = languageCode,
        )

        val result = catchApiErrorAndSendEvent(
            errorEvent = MarketsDataAnalyticsEvent.Details.Error(
                request = MarketsDataAnalyticsEvent.Details.Error.Request.Info,
                tokenSymbol = tokenSymbol,
            ),
        ) {
            response.getOrThrow()
        }

        val resultResponse = result.applyL2Compatibility(tokenId)
        return@withContext TokenMarketInfoConverter.convert(resultResponse)
    }

    override suspend fun getTokenQuotes(fiatCurrencyCode: String, tokenId: String, tokenSymbol: String) =
        withContext(dispatcherProvider.io) {
            // for second markets iteration we should use extended api method with all required fields
            val response = tangemTechApi.getQuotes(
                currencyId = fiatCurrencyCode,
                coinIds = tokenId,
                fields = marketsQuoteFields.joinToString(separator = ","),
            )

            val result = catchApiErrorAndSendEvent(
                errorEvent = MarketsDataAnalyticsEvent.Details.Error(
                    request = MarketsDataAnalyticsEvent.Details.Error.Request.Info,
                    tokenSymbol = tokenSymbol,
                ),
            ) {
                response.getOrThrow()
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
            CryptoCurrencyFactory().createCoin(
                blockchain = blockchain,
                extraDerivationPath = null,
                derivationStyleProvider = userWallet.scanResponse.derivationStyleProvider,
            )
        } else {
            val currencyNetwork = getNetwork(
                blockchain = blockchain,
                extraDerivationPath = null,
                derivationStyleProvider = userWallet.scanResponse.derivationStyleProvider,
            ) ?: return null

            CryptoCurrencyFactory().createToken(
                network = currencyNetwork,
                rawId = token.id,
                name = token.name,
                symbol = token.symbol,
                decimals = network.decimalCount ?: error("Unknown decimal"),
                contractAddress = network.contractAddress!!,
            )
        }
    }

    private inline fun <T> catchApiErrorAndSendEvent(errorEvent: MarketsDataAnalyticsEvent, block: () -> T): T {
        return try {
            block()
        } catch (e: ApiResponseError.HttpException) {
            analyticsEventHandler.send(errorEvent.toEvent())
            throw e
        } catch (e: ApiResponseError.TimeoutException) {
            analyticsEventHandler.send(errorEvent.toEvent())
            throw e
        }
    }
}