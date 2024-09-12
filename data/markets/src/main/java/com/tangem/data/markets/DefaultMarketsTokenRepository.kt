package com.tangem.data.markets

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.compatibility.applyL2Compatibility
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.data.common.currency.CryptoCurrencyFactory
import com.tangem.data.common.currency.getNetwork
import com.tangem.data.common.utils.retryOnError
import com.tangem.data.markets.converters.TokenChartConverter
import com.tangem.data.markets.converters.TokenMarketInfoConverter
import com.tangem.data.markets.converters.TokenMarketListConverter
import com.tangem.data.markets.converters.toRequestParam
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.markets.TangemTechMarketsApi
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.common.util.derivationStyleProvider
import com.tangem.domain.markets.*
import com.tangem.domain.markets.repositories.MarketsTokenRepository
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.pagination.*
import com.tangem.pagination.fetcher.LimitOffsetBatchFetcher
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

internal class DefaultMarketsTokenRepository(
    private val marketsApi: TangemTechMarketsApi,
    private val tangemTechApi: TangemTechApi,
    private val userWalletsStore: UserWalletsStore,
    private val dispatcherProvider: CoroutineDispatcherProvider,
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
                    requestCall()
                } else {
                    retryOnError(priority = true) {
                        requestCall()
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
    ): TokenChart {
        val response = marketsApi.getCoinChart(
            currency = fiatCurrencyCode,
            coinId = tokenId,
            interval = interval.toRequestParam(),
        )

        return TokenChartConverter.convert(interval, response.getOrThrow())
    }

    override suspend fun getChartPreview(
        fiatCurrencyCode: String,
        interval: PriceChangeInterval,
        tokenId: String,
    ): TokenChart {
        val response = marketsApi.getCoinsListCharts(
            coinIds = tokenId,
            currency = fiatCurrencyCode,
            interval = interval.toRequestParam(),
        )

        val chart = response.getOrThrow()[tokenId] ?: error("No chart preview data for token $tokenId")

        return TokenChartConverter.convert(interval, chart)
    }

    override suspend fun getTokenInfo(
        fiatCurrencyCode: String,
        tokenId: String,
        languageCode: String,
    ): TokenMarketInfo {
        val response = marketsApi.getCoinMarketData(
            currency = fiatCurrencyCode,
            coinId = tokenId,
            language = languageCode,
        )

        val resultResponse = response.getOrThrow().applyL2Compatibility(tokenId)
        return TokenMarketInfoConverter.convert(resultResponse)
    }

    override suspend fun getTokenQuotes(fiatCurrencyCode: String, tokenId: String): TokenQuotes {
        // TODO change method when backend is ready
        val response = marketsApi.getCoinMarketData(
            currency = fiatCurrencyCode,
            coinId = tokenId,
            language = "en",
        )

        return TokenMarketInfoConverter.convert(response.getOrThrow()).quotes
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
}
