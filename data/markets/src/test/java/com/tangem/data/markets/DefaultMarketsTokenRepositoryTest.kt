package com.tangem.data.markets

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.core.local.datastore.RuntimeStateStore
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.data.common.cache.CacheRegistry
import com.tangem.domain.markets.CoinCategory
import com.tangem.store.datasource.markets.TangemTechMarketsApi
import com.tangem.store.datasource.markets.models.response.CoinCategoriesResponse
import com.tangem.store.datasource.markets.models.response.CoinCategoriesResponse.Category
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultMarketsTokenRepositoryTest {

    private val marketsApi: TangemTechMarketsApi = mockk()
    private val cacheRegistry: CacheRegistry = mockk()
    private val coinCategoriesStore: RuntimeStateStore<List<Category>> = RuntimeStateStore(defaultValue = emptyList())

    private val repository = DefaultMarketsTokenRepository(
        marketsApi = marketsApi,
        quotesFetcher = mockk(),
        userWalletsListRepository = mockk(),
        dispatcherProvider = TestingCoroutineDispatcherProvider(),
        analyticsEventHandler = mockk(relaxed = true),
        cacheRegistry = cacheRegistry,
        tokenExchangesStore = RuntimeStateStore(defaultValue = emptyList()),
        coinCategoriesStore = coinCategoriesStore,
        networkFactory = mockk(),
        excludedBlockchains = ExcludedBlockchains(),
    )

    @BeforeEach
    fun setup() {
        clearMocks(marketsApi, cacheRegistry)
        coinCategoriesStore.clear()
    }

    @Test
    fun `GIVEN expired cache WHEN getCoinCategories THEN the response is stored and returned`() = runTest {
        // Arrange
        givenCacheExpired()
        val dto = category(id = "42", code = "stocks", displayName = "Stocks")
        coEvery { marketsApi.getCoinCategories() } returns ApiResponse.Success(CoinCategoriesResponse(listOf(dto)))

        // Act
        val actual = repository.getCoinCategories()

        // Assert
        assertThat(actual).containsExactly(coinCategory(id = "42", code = "stocks", displayName = "Stocks"))
        assertThat(coinCategoriesStore.get().value).containsExactly(dto)
        coVerify(exactly = 1) { marketsApi.getCoinCategories() }
    }

    @Test
    fun `GIVEN valid cache WHEN getCoinCategories THEN the api is not called and stored categories are returned`() =
        runTest {
            // Arrange
            givenCacheValid()
            coinCategoriesStore.store(listOf(category(id = "7", code = "commodities", displayName = "Commodities")))

            // Act
            val actual = repository.getCoinCategories()

            // Assert
            assertThat(actual)
                .containsExactly(coinCategory(id = "7", code = "commodities", displayName = "Commodities"))
            coVerify(exactly = 0) { marketsApi.getCoinCategories() }
        }

    @Test
    fun `GIVEN valid cache and empty store WHEN getCoinCategories THEN an empty list is returned`() = runTest {
        // Arrange — a cache hit serves the store as-is, so a cold store yields nothing instead of refetching
        givenCacheValid()

        // Act
        val actual = repository.getCoinCategories()

        // Assert
        assertThat(actual).isEmpty()
        coVerify(exactly = 0) { marketsApi.getCoinCategories() }
    }

    @Test
    fun `GIVEN any call WHEN getCoinCategories THEN the categories cache key is used without skipping cache`() =
        runTest {
            // Arrange
            givenCacheValid()

            // Act
            repository.getCoinCategories()

            // Assert — a shared or typo'd key would cross-contaminate the sibling "coins/{id}/exchanges" cache
            coVerify(exactly = 1) {
                cacheRegistry.invokeOnExpire(
                    key = "coins/categories",
                    skipCache = false,
                    expireIn = any(),
                    block = any(),
                )
            }
        }

    @Test
    fun `GIVEN api error WHEN getCoinCategories THEN the error propagates and the store is untouched`() = runTest {
        // Arrange — the method is deliberately not total; the Either boundary lives in the use case
        givenCacheExpired()
        val cached = category(id = "7", code = "commodities", displayName = "Commodities")
        coinCategoriesStore.store(listOf(cached))
        coEvery { marketsApi.getCoinCategories() } returns networkError()

        // Act
        val error = runCatching { repository.getCoinCategories() }.exceptionOrNull()

        // Assert
        assertThat(error).isInstanceOf(ApiResponseError.NetworkException::class.java)
        assertThat(coinCategoriesStore.get().value).containsExactly(cached)
    }

    private fun givenCacheExpired() {
        coEvery { cacheRegistry.invokeOnExpire(any(), any(), any(), any()) } coAnswers {
            arg<suspend () -> Unit>(3).invoke()
        }
    }

    private fun givenCacheValid() {
        coEvery { cacheRegistry.invokeOnExpire(any(), any(), any(), any()) } returns Unit
    }

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
}