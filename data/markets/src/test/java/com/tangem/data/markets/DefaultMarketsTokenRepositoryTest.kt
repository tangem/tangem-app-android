package com.tangem.data.markets

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.core.local.datastore.RuntimeStateStore
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.data.common.cache.CacheRegistry
import com.tangem.domain.markets.CoinCategory
import com.tangem.domain.markets.MarketCategory
import com.tangem.domain.markets.MarketCategoryListConfig
import com.tangem.domain.markets.MarketCategoryPage
import com.tangem.domain.markets.TokenMarketListConfig
import com.tangem.domain.markets.TokenMarketUpdateRequest
import com.tangem.store.datasource.markets.TangemTechMarketsApi
import com.tangem.store.datasource.markets.models.response.CoinCategoriesResponse
import com.tangem.store.datasource.markets.models.response.CoinCategoriesResponse.Category
import com.tangem.store.datasource.markets.models.response.MarketCategoriesResponse
import com.tangem.store.datasource.markets.models.response.TokenMarketListResponse
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchingContext
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import com.tangem.test.core.ProvideTestModels
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultMarketsTokenRepositoryTest {

    private val marketsApi: TangemTechMarketsApi = mockk()
    private val cacheRegistry: CacheRegistry = mockk()

    private val repository = DefaultMarketsTokenRepository(
        marketsApi = marketsApi,
        quotesFetcher = mockk(),
        userWalletsListRepository = mockk(),
        dispatcherProvider = TestingCoroutineDispatcherProvider(),
        analyticsEventHandler = mockk(relaxed = true),
        cacheRegistry = cacheRegistry,
        tokenExchangesStore = RuntimeStateStore(defaultValue = emptyList()),
        networkFactory = mockk(),
        excludedBlockchains = ExcludedBlockchains(),
    )

    @BeforeEach
    fun setup() {
        clearMocks(marketsApi, cacheRegistry)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetCoinCategories {

        @Test
        fun `GIVEN a country WHEN getCoinCategories THEN it travels in the request`() = runTest {
            // Arrange
            coEvery { marketsApi.getCoinCategories(country = "US") } returns
                ApiResponse.Success(CoinCategoriesResponse(listOf(category(id = "42"))))

            // Act
            val actual = repository.getCoinCategories(country = "US")

            // Assert
            assertThat(actual).containsExactly(coinCategory(id = "42"))
            coVerify(exactly = 1) { marketsApi.getCoinCategories(country = "US") }
        }

        @Test
        fun `GIVEN no country WHEN getCoinCategories THEN the request carries none`() = runTest {
            // Arrange
            coEvery { marketsApi.getCoinCategories(country = null) } returns
                ApiResponse.Success(CoinCategoriesResponse(listOf(category(id = "42"))))

            // Act
            val actual = repository.getCoinCategories(country = null)

            // Assert
            assertThat(actual).containsExactly(coinCategory(id = "42"))
            coVerify(exactly = 1) { marketsApi.getCoinCategories(country = null) }
        }

        @Test
        fun `GIVEN repeated calls WHEN getCoinCategories THEN every one reaches the api`() = runTest {
            // Arrange — the endpoint is deliberately uncached: the restriction flags it carries must never
            // outlive the answer they came with
            coEvery { marketsApi.getCoinCategories(country = "US") } returns
                ApiResponse.Success(CoinCategoriesResponse(listOf(category(id = "42"))))

            // Act
            repository.getCoinCategories(country = "US")
            repository.getCoinCategories(country = "US")

            // Assert
            coVerify(exactly = 2) { marketsApi.getCoinCategories(country = "US") }
            coVerify(exactly = 0) { cacheRegistry.invokeOnExpire(any(), any(), any(), any()) }
        }

        @Test
        fun `GIVEN a failing request WHEN getCoinCategories THEN the error propagates`() = runTest {
            // Arrange — the method is deliberately not total; the Either boundary lives in the use case
            coEvery { marketsApi.getCoinCategories(country = "US") } returns networkError()

            // Act
            val error = runCatching { repository.getCoinCategories(country = "US") }.exceptionOrNull()

            // Assert
            assertThat(error).isInstanceOf(ApiResponseError.NetworkException::class.java)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetMarketCategories {

        @Test
        fun `GIVEN api returns a page WHEN getMarketCategories THEN the page is converted and returned`() = runTest {
            // Arrange
            val response = marketCategoriesResponse(categories = listOf(marketCategoryDto(id = "layer-2")))
            coEvery {
                marketsApi.getMarketCategories(any(), any(), any(), any(), any(), any())
            } returns ApiResponse.Success(response)

            // Act
            val actual = repository.getMarketCategories(currency = "usd", offset = 0, limit = 20)

            // Assert
            assertThat(actual).isEqualTo(
                MarketCategoryPage(
                    categories = listOf(marketCategory(id = "layer-2")),
                    total = 42,
                    offset = 0,
                    limit = 20,
                ),
            )
        }

        @Test
        fun `GIVEN a category with no market data WHEN getMarketCategories THEN the nulls are preserved`() = runTest {
            // Arrange — thin categories come back without aggregated market data
            val dto = marketCategoryDto(
                id = "new-listings",
                description = null,
                marketCap = null,
                marketCapChange24h = null,
                volume24h = null,
            )
            coEvery {
                marketsApi.getMarketCategories(any(), any(), any(), any(), any(), any())
            } returns ApiResponse.Success(marketCategoriesResponse(categories = listOf(dto)))

            // Act
            val actual = repository.getMarketCategories(currency = "usd", offset = 0, limit = 20)

            // Assert
            assertThat(actual.categories).containsExactly(
                marketCategory(
                    id = "new-listings",
                    description = null,
                    marketCap = null,
                    marketCapChangePercent24h = null,
                    volume24h = null,
                ),
            )
        }

        @Test
        fun `GIVEN a percent change WHEN getMarketCategories THEN it is converted to a fraction`() = runTest {
            // Arrange — the api sends -1.52 meaning -1.52%, while the domain carries fractions like TokenQuotesShort
            val dto = marketCategoryDto(marketCapChange24h = BigDecimal("-1.52"))
            coEvery {
                marketsApi.getMarketCategories(any(), any(), any(), any(), any(), any())
            } returns ApiResponse.Success(marketCategoriesResponse(categories = listOf(dto)))

            // Act
            val actual = repository.getMarketCategories(currency = "usd", offset = 0, limit = 20)

            // Assert — without this the UI percent formatter would render -152%
            assertThat(actual.categories.single().marketCapChangePercent24h).isEqualTo(BigDecimal("-0.0152"))
        }

        @Test
        fun `GIVEN a blank description WHEN getMarketCategories THEN it is normalized to null`() = runTest {
            // Arrange — the api returns "" and null interchangeably for a missing description
            val dto = marketCategoryDto(description = "   ")
            coEvery {
                marketsApi.getMarketCategories(any(), any(), any(), any(), any(), any())
            } returns ApiResponse.Success(marketCategoriesResponse(categories = listOf(dto)))

            // Act
            val actual = repository.getMarketCategories(currency = "usd", offset = 0, limit = 20)

            // Assert
            assertThat(actual.categories.single().description).isNull()
        }

        @Test
        fun `GIVEN paging arguments WHEN getMarketCategories THEN they are forwarded to the api`() = runTest {
            // Arrange
            coEvery {
                marketsApi.getMarketCategories(any(), any(), any(), any(), any(), any())
            } returns ApiResponse.Success(marketCategoriesResponse())

            // Act
            repository.getMarketCategories(
                currency = "eur",
                offset = 20,
                limit = 50,
                timestamp = 1754838000000L,
                featured = true,
            )

            // Assert
            coVerify(exactly = 1) {
                marketsApi.getMarketCategories(
                    currency = "eur",
                    offset = 20,
                    limit = 50,
                    timestamp = 1754838000000L,
                    featured = true,
                )
            }
        }

        @Test
        fun `GIVEN any call WHEN getMarketCategories THEN the cache registry is not used`() = runTest {
            // Arrange — a single-value store cannot hold more than one page, and expiry is disabled project-wide,
            // so caching this endpoint would make every page after the first unreachable
            coEvery {
                marketsApi.getMarketCategories(any(), any(), any(), any(), any(), any())
            } returns ApiResponse.Success(marketCategoriesResponse())

            // Act
            repository.getMarketCategories(currency = "usd", offset = 0, limit = 20)

            // Assert
            coVerify(exactly = 0) { cacheRegistry.invokeOnExpire(any(), any(), any(), any()) }
        }

        @Test
        fun `GIVEN api error WHEN getMarketCategories THEN the error propagates`() = runTest {
            // Arrange — the method is deliberately not total; the Either boundary lives in the use case
            coEvery { marketsApi.getMarketCategories(any(), any(), any(), any(), any(), any()) } returns networkError()

            // Act
            val error = runCatching {
                repository.getMarketCategories(currency = "usd", offset = 0, limit = 20)
            }.exceptionOrNull()

            // Assert
            assertThat(error).isInstanceOf(ApiResponseError.NetworkException::class.java)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetMarketCategoryListFlow {

        private fun provideTestModels() = listOf(
            FeaturedFilterModel(filter = MarketCategoryListConfig.FeaturedFilter.All, expected = null),
            FeaturedFilterModel(filter = MarketCategoryListConfig.FeaturedFilter.OnlyFeatured, expected = true),
            FeaturedFilterModel(filter = MarketCategoryListConfig.FeaturedFilter.WithoutFeatured, expected = false),
        )

        @Test
        fun `GIVEN two pages WHEN flow reloaded and load more THEN both batches are emitted in order`() = runTest {
            // Arrange
            coEvery {
                marketsApi.getMarketCategories(any(), offset = 0, any(), any(), any(), any())
            } returns ApiResponse.Success(
                marketCategoriesResponse(categories = listOf(marketCategoryDto(id = "first")), limit = 1),
            )
            coEvery {
                marketsApi.getMarketCategories(any(), offset = 1, any(), any(), any(), any())
            } returns ApiResponse.Success(
                marketCategoriesResponse(categories = listOf(marketCategoryDto(id = "second")), offset = 1, limit = 1),
            )
            val actions = MutableSharedFlow<BatchAction<Int, MarketCategoryListConfig, Nothing>>(replay = 1)
            val sourceScope = testSourceScope()
            val batchFlow = repository.getMarketCategoryListFlow(
                batchingContext = BatchingContext(actionsFlow = actions, coroutineScope = sourceScope),
                firstBatchSize = 1,
                nextBatchSize = 1,
            )

            // Act
            actions.emit(BatchAction.Reload(requestParams = usdConfig))
            advanceUntilIdle()
            actions.emit(BatchAction.LoadMore())
            advanceUntilIdle()

            // Assert
            val batches = batchFlow.state.value.data
            assertThat(batches.map { it.key }).containsExactly(0, 1).inOrder()
            assertThat(batches.flatMap { it.data }.map { it.id }).containsExactly("first", "second").inOrder()
            sourceScope.cancel()
        }

        @Test
        fun `GIVEN a config WHEN flow reloaded THEN currency featured and timestamp reach the api`() = runTest {
            // Arrange
            coEvery {
                marketsApi.getMarketCategories(any(), any(), any(), any(), any(), any())
            } returns ApiResponse.Success(marketCategoriesResponse())
            val actions = MutableSharedFlow<BatchAction<Int, MarketCategoryListConfig, Nothing>>(replay = 1)
            val sourceScope = testSourceScope()
            val batchFlow = repository.getMarketCategoryListFlow(
                batchingContext = BatchingContext(actionsFlow = actions, coroutineScope = sourceScope),
                firstBatchSize = 7,
                nextBatchSize = 3,
            )

            // Act
            actions.emit(
                BatchAction.Reload(
                    requestParams = MarketCategoryListConfig(
                        fiatCurrencySymbol = "eur",
                        featuredFilter = MarketCategoryListConfig.FeaturedFilter.OnlyFeatured,
                        timestamp = 1754838000000L,
                    ),
                ),
            )
            advanceUntilIdle()

            // Assert — the first batch uses prefetchDistance, not batchSize
            coVerify(exactly = 1) {
                marketsApi.getMarketCategories(
                    currency = "eur",
                    offset = 0,
                    limit = 7,
                    timestamp = 1754838000000L,
                    featured = true,
                    search = null,
                )
            }
            assertThat(batchFlow.state.value.data).hasSize(1)
            sourceScope.cancel()
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a featured filter WHEN flow reloaded THEN it maps to the featured query param`(
            model: FeaturedFilterModel,
        ) = runTest {
            // Arrange — a tri-state filter: no param at all means "every category"
            coEvery {
                marketsApi.getMarketCategories(any(), any(), any(), any(), any(), any())
            } returns ApiResponse.Success(marketCategoriesResponse())
            val actions = MutableSharedFlow<BatchAction<Int, MarketCategoryListConfig, Nothing>>(replay = 1)
            val sourceScope = testSourceScope()
            repository.getMarketCategoryListFlow(
                batchingContext = BatchingContext(actionsFlow = actions, coroutineScope = sourceScope),
                firstBatchSize = 20,
                nextBatchSize = 20,
            )

            // Act
            actions.emit(BatchAction.Reload(requestParams = usdConfig.copy(featuredFilter = model.filter)))
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) {
                marketsApi.getMarketCategories(any(), any(), any(), any(), featured = model.expected, search = any())
            }
            sourceScope.cancel()
        }

        @Test
        fun `GIVEN every page pinned to one snapshot WHEN paging THEN the timestamp does not change`() = runTest {
            // Arrange — the backend issues no snapshot token to echo, so the config carries it for the whole listing
            coEvery {
                marketsApi.getMarketCategories(any(), any(), any(), any(), any(), any())
            } returns ApiResponse.Success(
                marketCategoriesResponse(categories = listOf(marketCategoryDto()), limit = 1),
            )
            val actions = MutableSharedFlow<BatchAction<Int, MarketCategoryListConfig, Nothing>>(replay = 1)
            val sourceScope = testSourceScope()
            val batchFlow = repository.getMarketCategoryListFlow(
                batchingContext = BatchingContext(actionsFlow = actions, coroutineScope = sourceScope),
                firstBatchSize = 1,
                nextBatchSize = 1,
            )

            // Act
            actions.emit(BatchAction.Reload(requestParams = usdConfig.copy(timestamp = 1754838000000L)))
            advanceUntilIdle()
            actions.emit(BatchAction.LoadMore())
            advanceUntilIdle()

            // Assert
            assertThat(batchFlow.state.value.data).hasSize(2)
            coVerify(exactly = 2) {
                marketsApi.getMarketCategories(any(), any(), any(), timestamp = 1754838000000L, any(), any())
            }
        }

        @Test
        fun `GIVEN a short page WHEN flow reloaded THEN pagination ends`() = runTest {
            // Arrange — fewer items than requested is the only end-of-list signal the backend gives
            coEvery {
                marketsApi.getMarketCategories(any(), any(), any(), any(), any(), any())
            } returns ApiResponse.Success(
                marketCategoriesResponse(categories = listOf(marketCategoryDto()), limit = 20),
            )
            val actions = MutableSharedFlow<BatchAction<Int, MarketCategoryListConfig, Nothing>>(replay = 1)
            val sourceScope = testSourceScope()
            val batchFlow = repository.getMarketCategoryListFlow(
                batchingContext = BatchingContext(actionsFlow = actions, coroutineScope = sourceScope),
                firstBatchSize = 20,
                nextBatchSize = 20,
            )

            // Act
            actions.emit(BatchAction.Reload(requestParams = usdConfig))
            advanceUntilIdle()

            // Assert
            assertThat(batchFlow.state.value.status).isInstanceOf(PaginationStatus.EndOfPagination::class.java)
            sourceScope.cancel()
        }

        @Test
        fun `GIVEN a past-the-end page WHEN load more THEN the empty batch is not appended`() = runTest {
            // Arrange — offset beyond total answers 200 with an empty list, not an error
            coEvery {
                marketsApi.getMarketCategories(any(), offset = 0, any(), any(), any(), any())
            } returns ApiResponse.Success(
                marketCategoriesResponse(categories = listOf(marketCategoryDto()), limit = 1),
            )
            coEvery {
                marketsApi.getMarketCategories(any(), offset = 1, any(), any(), any(), any())
            } returns ApiResponse.Success(marketCategoriesResponse(categories = emptyList(), offset = 1, limit = 1))
            val actions = MutableSharedFlow<BatchAction<Int, MarketCategoryListConfig, Nothing>>(replay = 1)
            val sourceScope = testSourceScope()
            val batchFlow = repository.getMarketCategoryListFlow(
                batchingContext = BatchingContext(actionsFlow = actions, coroutineScope = sourceScope),
                firstBatchSize = 1,
                nextBatchSize = 1,
            )

            // Act
            actions.emit(BatchAction.Reload(requestParams = usdConfig))
            advanceUntilIdle()
            actions.emit(BatchAction.LoadMore())
            advanceUntilIdle()

            // Assert
            assertThat(batchFlow.state.value.data).hasSize(1)
            sourceScope.cancel()
        }

        @ParameterizedTest
        @MethodSource("provideSearchTextModels")
        fun `GIVEN a search text WHEN flow reloaded THEN it reaches the api normalized`(model: SearchTextModel) =
            runTest {
                // Arrange — a blank query is no query: it must not narrow the listing to nothing
                coEvery {
                    marketsApi.getMarketCategories(any(), any(), any(), any(), any(), any())
                } returns ApiResponse.Success(marketCategoriesResponse())
                val actions = MutableSharedFlow<BatchAction<Int, MarketCategoryListConfig, Nothing>>(replay = 1)
                val sourceScope = testSourceScope()
                repository.getMarketCategoryListFlow(
                    batchingContext = BatchingContext(actionsFlow = actions, coroutineScope = sourceScope),
                    firstBatchSize = 20,
                    nextBatchSize = 20,
                )

                // Act
                actions.emit(BatchAction.Reload(requestParams = usdConfig.copy(searchText = model.searchText)))
                advanceUntilIdle()

                // Assert
                coVerify(exactly = 1) {
                    marketsApi.getMarketCategories(any(), any(), any(), any(), any(), search = model.expected)
                }
                sourceScope.cancel()
            }

        @Test
        fun `GIVEN a search text WHEN the next page is loaded THEN it still carries the query`() = runTest {
            // Arrange
            coEvery {
                marketsApi.getMarketCategories(any(), any(), any(), any(), any(), any())
            } returns ApiResponse.Success(
                marketCategoriesResponse(categories = listOf(marketCategoryDto()), limit = 1),
            )
            val actions = MutableSharedFlow<BatchAction<Int, MarketCategoryListConfig, Nothing>>(replay = 1)
            val sourceScope = testSourceScope()
            repository.getMarketCategoryListFlow(
                batchingContext = BatchingContext(actionsFlow = actions, coroutineScope = sourceScope),
                firstBatchSize = 1,
                nextBatchSize = 1,
            )

            // Act
            actions.emit(BatchAction.Reload(requestParams = usdConfig.copy(searchText = "meme")))
            advanceUntilIdle()
            actions.emit(BatchAction.LoadMore())
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 2) {
                marketsApi.getMarketCategories(any(), any(), any(), any(), any(), search = "meme")
            }
            sourceScope.cancel()
        }

        private fun provideSearchTextModels() = listOf(
            SearchTextModel(searchText = "meme", expected = "meme"),
            SearchTextModel(searchText = null, expected = null),
            SearchTextModel(searchText = "", expected = null),
            SearchTextModel(searchText = "   ", expected = null),
        )

        @Test
        fun `GIVEN a failing first page WHEN flow reloaded THEN the error is reported without a retry`() = runTest {
            // Arrange — unlike the token list this fetcher does not retryOnError, so a dead network surfaces
            coEvery { marketsApi.getMarketCategories(any(), any(), any(), any(), any(), any()) } returns networkError()
            val actions = MutableSharedFlow<BatchAction<Int, MarketCategoryListConfig, Nothing>>(replay = 1)
            val sourceScope = testSourceScope()
            val batchFlow = repository.getMarketCategoryListFlow(
                batchingContext = BatchingContext(actionsFlow = actions, coroutineScope = sourceScope),
                firstBatchSize = 20,
                nextBatchSize = 20,
            )

            // Act
            actions.emit(BatchAction.Reload(requestParams = usdConfig))
            advanceUntilIdle()

            // Assert
            val status = batchFlow.state.value.status
            assertThat(status).isInstanceOf(PaginationStatus.InitialLoadingError::class.java)
            assertThat((status as PaginationStatus.InitialLoadingError).throwable)
                .isInstanceOf(ApiResponseError.NetworkException::class.java)
            coVerify(exactly = 1) { marketsApi.getMarketCategories(any(), any(), any(), any(), any(), any()) }
            sourceScope.cancel()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetTokenListFlow {

        @Test
        fun `GIVEN a category filter WHEN the list is reloaded THEN its id reaches the api`() = runTest {
            // Arrange
            coEvery {
                marketsApi.getCoinsList(
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                )
            } returns ApiResponse.Success(tokenMarketListResponse())
            val actions = MutableSharedFlow<BatchAction<Int, TokenMarketListConfig, TokenMarketUpdateRequest>>(
                replay = 1,
            )
            val sourceScope = testSourceScope()
            repository.getTokenListFlow(
                batchingContext = BatchingContext(actionsFlow = actions, coroutineScope = sourceScope),
                firstBatchSize = 1,
                nextBatchSize = 1,
            )

            // Act
            actions.emit(BatchAction.Reload(requestParams = listConfig(geckoCategoryId = "layer-1")))
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) {
                marketsApi.getCoinsList(
                    currency = "USD",
                    interval = any(),
                    offset = 0,
                    limit = 1,
                    order = any(),
                    search = null,
                    timestamp = null,
                    showNetworks = any(),
                    categoryId = null,
                    sectorId = null,
                    geckoCategoryId = "layer-1",
                )
            }
            sourceScope.cancel()
        }

        @Test
        fun `GIVEN a country WHEN the list is reloaded THEN it travels to the api`() = runTest {
            // Arrange — without it every token comes back unrestricted, so the region marks never appear
            coEvery {
                marketsApi.getCoinsList(
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                )
            } returns ApiResponse.Success(tokenMarketListResponse())
            val actions = MutableSharedFlow<BatchAction<Int, TokenMarketListConfig, TokenMarketUpdateRequest>>(
                replay = 1,
            )
            val sourceScope = testSourceScope()
            repository.getTokenListFlow(
                batchingContext = BatchingContext(actionsFlow = actions, coroutineScope = sourceScope),
                firstBatchSize = 1,
                nextBatchSize = 1,
            )

            // Act
            actions.emit(BatchAction.Reload(requestParams = listConfig(country = "GB")))
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) {
                marketsApi.getCoinsList(
                    currency = "USD",
                    interval = any(),
                    offset = 0,
                    limit = 1,
                    order = any(),
                    search = null,
                    timestamp = null,
                    showNetworks = any(),
                    categoryId = null,
                    sectorId = null,
                    geckoCategoryId = null,
                    country = "GB",
                )
            }
            sourceScope.cancel()
        }

        @Test
        fun `GIVEN no category filter WHEN the list is reloaded THEN the api is not narrowed to a category`() =
            runTest {
                // Arrange
                coEvery {
                    marketsApi.getCoinsList(
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                )
                } returns ApiResponse.Success(tokenMarketListResponse())
                val actions = MutableSharedFlow<BatchAction<Int, TokenMarketListConfig, TokenMarketUpdateRequest>>(
                    replay = 1,
                )
                val sourceScope = testSourceScope()
                repository.getTokenListFlow(
                    batchingContext = BatchingContext(actionsFlow = actions, coroutineScope = sourceScope),
                    firstBatchSize = 1,
                    nextBatchSize = 1,
                )

                // Act
                actions.emit(BatchAction.Reload(requestParams = listConfig(geckoCategoryId = null)))
                advanceUntilIdle()

                // Assert
                coVerify(exactly = 1) {
                    marketsApi.getCoinsList(
                        currency = any(),
                        interval = any(),
                        offset = any(),
                        limit = any(),
                        order = any(),
                        search = any(),
                        timestamp = any(),
                        showNetworks = any(),
                        categoryId = any(),
                        sectorId = any(),
                        geckoCategoryId = null,
                    )
                }
                sourceScope.cancel()
            }
    }

    private fun listConfig(
        fiatPriceCurrency: String = "USD",
        searchText: String? = null,
        priceChangeInterval: TokenMarketListConfig.Interval = TokenMarketListConfig.Interval.H24,
        order: TokenMarketListConfig.Order = TokenMarketListConfig.Order.ByRating,
        geckoCategoryId: String? = null,
        country: String? = null,
    ) = TokenMarketListConfig(
        fiatPriceCurrency = fiatPriceCurrency,
        searchText = searchText,
        priceChangeInterval = priceChangeInterval,
        order = order,
        geckoCategoryId = geckoCategoryId,
        country = country,
    )

    private fun tokenMarketListResponse(
        tokens: List<TokenMarketListResponse.Token> = emptyList(),
        total: Int = tokens.size,
        limit: Int = 1,
        offset: Int = 0,
    ) = TokenMarketListResponse(
        imageHost = null,
        tokens = tokens,
        total = total,
        limit = limit,
        offset = offset,
    )

    /**
     * A scope for the batch source, driven by the scheduler of the test. Deliberately NOT
     * [TestScope.backgroundScope]: its tasks carry the background marker, which [advanceUntilIdle] does not run.
     * The job is detached so the source's never-completing collectors do not keep [runTest] waiting; cancel the
     * scope at the end of the test.
     */
    private fun TestScope.testSourceScope(): CoroutineScope = CoroutineScope(coroutineContext + Job())

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> networkError(): ApiResponse<T> =
        ApiResponse.Error(cause = ApiResponseError.NetworkException()) as ApiResponse<T>

    private fun category(
        id: String = "42",
        code: String = "stocks",
        displayName: String = "Stocks",
        displayOrder: Int = 1,
        tokensCount: Int = 5,
        isRestricted: Boolean = false,
        isVisible: Boolean = true,
        sectors: List<Category.Sector> = emptyList(),
    ) = Category(
        id = id,
        code = code,
        displayName = displayName,
        displayOrder = displayOrder,
        tokensCount = tokensCount,
        isRestricted = isRestricted,
        isVisible = isVisible,
        sectors = sectors,
    )

    private fun coinCategory(
        id: String = "42",
        code: String = "stocks",
        displayName: String = "Stocks",
        displayOrder: Int = 1,
        tokensCount: Int = 5,
        isRestricted: Boolean = false,
        isVisible: Boolean = true,
        sectors: List<CoinCategory.Sector> = emptyList(),
    ) = CoinCategory(
        id = id,
        code = code,
        displayName = displayName,
        displayOrder = displayOrder,
        tokensCount = tokensCount,
        isRestricted = isRestricted,
        isVisible = isVisible,
        sectors = sectors,
    )

    private val usdConfig = MarketCategoryListConfig(fiatCurrencySymbol = "usd")

    internal data class FeaturedFilterModel(
        val filter: MarketCategoryListConfig.FeaturedFilter,
        val expected: Boolean?,
    )

    internal data class SearchTextModel(val searchText: String?, val expected: String?)

    private fun marketCategoriesResponse(
        categories: List<MarketCategoriesResponse.Category> = listOf(marketCategoryDto()),
        total: Int = 42,
        offset: Int = 0,
        limit: Int = 20,
    ) = MarketCategoriesResponse(
        categories = categories,
        total = total,
        offset = offset,
        limit = limit,
    )

    private fun marketCategoryDto(
        id: String = "layer-2",
        name: String = "Layer 2",
        description: String? = "Scaling solutions",
        marketCap: BigDecimal? = BigDecimal("1000"),
        marketCapChange24h: BigDecimal? = BigDecimal("-25"),
        volume24h: BigDecimal? = BigDecimal("500"),
        isFeatured: Boolean = true,
        tokensCount: Int = 7,
    ) = MarketCategoriesResponse.Category(
        id = id,
        name = name,
        description = description,
        marketCap = marketCap,
        marketCapChange24h = marketCapChange24h,
        volume24h = volume24h,
        isFeatured = isFeatured,
        tokensCount = tokensCount,
    )

    private fun marketCategory(
        id: String = "layer-2",
        name: String = "Layer 2",
        description: String? = "Scaling solutions",
        marketCap: BigDecimal? = BigDecimal("1000"),
        marketCapChangePercent24h: BigDecimal? = BigDecimal("-0.25"),
        volume24h: BigDecimal? = BigDecimal("500"),
        isFeatured: Boolean = true,
        tokensCount: Int = 7,
    ) = MarketCategory(
        id = id,
        name = name,
        description = description,
        marketCap = marketCap,
        marketCapChangePercent24h = marketCapChangePercent24h,
        volume24h = volume24h,
        isFeatured = isFeatured,
        tokensCount = tokensCount,
    )
}