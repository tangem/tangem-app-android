package com.tangem.data.yield.supply.incentives

import arrow.core.Either
import arrow.core.left
import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.data.yield.supply.incentives.cache.DefaultYieldIncentivesCache
import com.tangem.domain.yield.supply.incentives.YieldIncentivesRepository
import com.tangem.domain.yield.supply.models.incentives.YieldIncentivesError
import com.tangem.grow.datasource.yield.YieldIncentivesApi
import com.tangem.grow.datasource.yield.models.YieldMarketsResponse
import com.tangem.grow.datasource.yield.models.incentives.YieldAprHistoryPointDto
import com.tangem.grow.datasource.yield.models.incentives.YieldAprHistoryResponse
import com.tangem.grow.datasource.yield.models.incentives.YieldClaimCalldataResponse
import com.tangem.grow.datasource.yield.models.incentives.YieldClaimHistoryResponse
import com.tangem.grow.datasource.yield.models.incentives.YieldIncentivesResponse
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.called
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultYieldIncentivesRepositoryTest {

    private val api: YieldIncentivesApi = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(api)
    }

    @Test
    fun `GIVEN 400 response WHEN getIncentives THEN feature absent`() = runTest {
        // Arrange
        val repository = createRepository(testScope = this)
        coEvery { api.getIncentives(chainId = CHAIN_ID, userAddress = ADDRESS) } returns badRequest()

        // Act
        val actual = repository.getIncentives(chainId = CHAIN_ID, userAddress = ADDRESS)

        // Assert
        assertThat(actual).isEqualTo(YieldIncentivesError.FeatureAbsent.left())
    }

    @Test
    fun `GIVEN network error WHEN getIncentives THEN unavailable`() = runTest {
        // Arrange
        val repository = createRepository(testScope = this)
        coEvery { api.getIncentives(chainId = CHAIN_ID, userAddress = ADDRESS) } returns
            networkError()

        // Act
        val actual = repository.getIncentives(chainId = CHAIN_ID, userAddress = ADDRESS)

        // Assert
        assertThat(actual).isEqualTo(YieldIncentivesError.Unavailable.left())
    }

    @Test
    fun `GIVEN unparseable timestamp WHEN getIncentives THEN unavailable instead of throwing`() = runTest {
        // Arrange
        val repository = createRepository(testScope = this)
        coEvery { api.getIncentives(chainId = CHAIN_ID, userAddress = ADDRESS) } returns
            ApiResponse.Success(incentivesResponse(lastUpdatedAt = "yesterday"))

        // Act
        val actual = repository.getIncentives(chainId = CHAIN_ID, userAddress = ADDRESS)

        // Assert
        assertThat(actual).isEqualTo(YieldIncentivesError.Unavailable.left())
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN toggle disabled WHEN any method is called THEN feature absent and api untouched`(
        model: ToggleOffModel,
    ) = runTest {
        // Arrange
        val repository = createRepository(testScope = this, isMerklIncentivesEnabled = false)

        // Act
        val actual = model.call(repository)

        // Assert
        assertThat(actual).isEqualTo(YieldIncentivesError.FeatureAbsent.left())
        coVerify { api wasNot called }
    }

    internal data class ToggleOffModel(
        val name: String,
        val call: suspend (YieldIncentivesRepository) -> Either<YieldIncentivesError, Any>,
    ) {
        override fun toString(): String = name
    }

    private fun provideTestModels() = listOf(
        ToggleOffModel("getIncentiveMarkets") { it.getIncentiveMarkets(chainIds = listOf(CHAIN_ID)) },
        ToggleOffModel("getIncentives") { it.getIncentives(chainId = CHAIN_ID, userAddress = ADDRESS) },
        ToggleOffModel("getAprHistory") { it.getAprHistory(chainId = CHAIN_ID, tokenAddress = TOKEN, days = 30) },
        ToggleOffModel("getClaimCalldata") { it.getClaimCalldata(chainId = CHAIN_ID, userAddress = ADDRESS) },
        ToggleOffModel("getClaimHistory") { it.getClaimHistory(chainId = CHAIN_ID, userAddress = ADDRESS) },
    )

    @Test
    fun `GIVEN cached incentives within 30s WHEN getIncentives twice THEN api called once`() = runTest {
        // Arrange
        val repository = createRepository(testScope = this)
        coEvery { api.getIncentives(chainId = CHAIN_ID, userAddress = ADDRESS) } returns
            ApiResponse.Success(incentivesResponse())

        // Act
        repository.getIncentives(chainId = CHAIN_ID, userAddress = ADDRESS)
        advanceTimeBy(delayTimeMillis = 29_000)
        repository.getIncentives(chainId = CHAIN_ID, userAddress = ADDRESS)

        // Assert
        coVerify(exactly = 1) { api.getIncentives(chainId = CHAIN_ID, userAddress = ADDRESS) }
    }

    @Test
    fun `GIVEN cache window elapsed WHEN getIncentives twice THEN api called twice`() = runTest {
        // Arrange
        val repository = createRepository(testScope = this)
        coEvery { api.getIncentives(chainId = CHAIN_ID, userAddress = ADDRESS) } returns
            ApiResponse.Success(incentivesResponse())

        // Act
        repository.getIncentives(chainId = CHAIN_ID, userAddress = ADDRESS)
        advanceTimeBy(delayTimeMillis = 31_000)
        repository.getIncentives(chainId = CHAIN_ID, userAddress = ADDRESS)

        // Assert
        coVerify(exactly = 2) { api.getIncentives(chainId = CHAIN_ID, userAddress = ADDRESS) }
    }

    @Test
    fun `GIVEN failed request WHEN getIncentives twice THEN failure is not cached`() = runTest {
        // Arrange
        val repository = createRepository(testScope = this)
        coEvery { api.getIncentives(chainId = CHAIN_ID, userAddress = ADDRESS) } returns badRequest()

        // Act
        repository.getIncentives(chainId = CHAIN_ID, userAddress = ADDRESS)
        repository.getIncentives(chainId = CHAIN_ID, userAddress = ADDRESS)

        // Assert
        coVerify(exactly = 2) { api.getIncentives(chainId = CHAIN_ID, userAddress = ADDRESS) }
    }

    @Test
    fun `GIVEN concurrent callers WHEN getIncentives THEN request is joined`() = runTest {
        // Arrange
        val repository = createRepository(testScope = this)
        coEvery { api.getIncentives(chainId = CHAIN_ID, userAddress = ADDRESS) } coAnswers {
            delay(timeMillis = 1_000)
            ApiResponse.Success(incentivesResponse())
        }

        // Act
        listOf(
            async { repository.getIncentives(chainId = CHAIN_ID, userAddress = ADDRESS) },
            async { repository.getIncentives(chainId = CHAIN_ID, userAddress = ADDRESS) },
        ).awaitAll()

        // Assert
        coVerify(exactly = 1) { api.getIncentives(chainId = CHAIN_ID, userAddress = ADDRESS) }
    }

    @Test
    fun `GIVEN claim calldata fetched twice WHEN getClaimCalldata THEN api called twice`() = runTest {
        // Arrange
        val repository = createRepository(testScope = this)
        coEvery { api.getClaimCalldata(chainId = CHAIN_ID, userAddress = ADDRESS) } returns
            ApiResponse.Success(YieldClaimCalldataResponse(chainId = CHAIN_ID, hasClaimable = false))

        // Act
        repository.getClaimCalldata(chainId = CHAIN_ID, userAddress = ADDRESS)
        repository.getClaimCalldata(chainId = CHAIN_ID, userAddress = ADDRESS)

        // Assert
        coVerify(exactly = 2) { api.getClaimCalldata(chainId = CHAIN_ID, userAddress = ADDRESS) }
    }

    @Test
    fun `GIVEN unparseable date WHEN getAprHistory THEN unavailable instead of throwing`() = runTest {
        // Arrange
        val repository = createRepository(testScope = this)
        coEvery { api.getAprHistory(chainId = CHAIN_ID, tokenAddress = TOKEN, days = any()) } returns
            ApiResponse.Success(
                YieldAprHistoryResponse(
                    chainId = CHAIN_ID,
                    tokenAddress = TOKEN,
                    points = listOf(YieldAprHistoryPointDto(date = "31 July")),
                ),
            )

        // Act
        val actual = repository.getAprHistory(chainId = CHAIN_ID, tokenAddress = TOKEN, days = 30)

        // Assert
        assertThat(actual).isEqualTo(YieldIncentivesError.Unavailable.left())
    }

    @Test
    fun `GIVEN days above the contract maximum WHEN getAprHistory THEN days are coerced`() = runTest {
        // Arrange
        val repository = createRepository(testScope = this)
        coEvery { api.getAprHistory(chainId = CHAIN_ID, tokenAddress = TOKEN, days = any()) } returns
            ApiResponse.Success(YieldAprHistoryResponse(chainId = CHAIN_ID, tokenAddress = TOKEN, points = null))

        // Act
        repository.getAprHistory(chainId = CHAIN_ID, tokenAddress = TOKEN, days = 180)

        // Assert
        coVerify(exactly = 1) { api.getAprHistory(chainId = CHAIN_ID, tokenAddress = TOKEN, days = 90) }
    }

    @Test
    fun `GIVEN different periods WHEN getAprHistory THEN each period is cached separately`() = runTest {
        // Arrange
        val repository = createRepository(testScope = this)
        coEvery { api.getAprHistory(chainId = CHAIN_ID, tokenAddress = TOKEN, days = any()) } returns
            ApiResponse.Success(YieldAprHistoryResponse(chainId = CHAIN_ID, tokenAddress = TOKEN, points = null))

        // Act
        repository.getAprHistory(chainId = CHAIN_ID, tokenAddress = TOKEN, days = 7)
        repository.getAprHistory(chainId = CHAIN_ID, tokenAddress = TOKEN, days = 7)
        repository.getAprHistory(chainId = CHAIN_ID, tokenAddress = TOKEN, days = 90)

        // Assert
        coVerify(exactly = 1) { api.getAprHistory(chainId = CHAIN_ID, tokenAddress = TOKEN, days = 7) }
        coVerify(exactly = 1) { api.getAprHistory(chainId = CHAIN_ID, tokenAddress = TOKEN, days = 90) }
    }

    @Test
    fun `GIVEN claim history within 60s WHEN getClaimHistory twice THEN api called once`() = runTest {
        // Arrange
        val repository = createRepository(testScope = this)
        coEvery { api.getClaimHistory(chainId = CHAIN_ID, userAddress = ADDRESS) } returns
            ApiResponse.Success(YieldClaimHistoryResponse(chainId = CHAIN_ID, claims = null))

        // Act
        repository.getClaimHistory(chainId = CHAIN_ID, userAddress = ADDRESS)
        advanceTimeBy(delayTimeMillis = 59_000)
        repository.getClaimHistory(chainId = CHAIN_ID, userAddress = ADDRESS)

        // Assert
        coVerify(exactly = 1) { api.getClaimHistory(chainId = CHAIN_ID, userAddress = ADDRESS) }
    }

    @Test
    fun `GIVEN chain ids WHEN getIncentiveMarkets THEN they are sent as a comma separated query`() = runTest {
        // Arrange
        val repository = createRepository(testScope = this)
        coEvery { api.getIncentiveMarkets(chainId = any(), isForEarn = null) } returns
            ApiResponse.Success(YieldMarketsResponse(marketDtos = emptyList(), lastUpdated = TIMESTAMP))

        // Act
        repository.getIncentiveMarkets(chainIds = listOf(137, 1))

        // Assert
        coVerify(exactly = 1) { api.getIncentiveMarkets(chainId = "1,137", isForEarn = null) }
    }

    @Test
    fun `GIVEN no chain ids WHEN getIncentiveMarkets THEN the query is omitted`() = runTest {
        // Arrange
        val repository = createRepository(testScope = this)
        coEvery { api.getIncentiveMarkets(chainId = any(), isForEarn = null) } returns
            ApiResponse.Success(YieldMarketsResponse(marketDtos = emptyList(), lastUpdated = TIMESTAMP))

        // Act
        repository.getIncentiveMarkets(chainIds = emptyList())

        // Assert
        coVerify(exactly = 1) { api.getIncentiveMarkets(chainId = null, isForEarn = null) }
    }

    private fun createRepository(testScope: TestScope, isMerklIncentivesEnabled: Boolean = true) =
        DefaultYieldIncentivesRepository(
            api = api,
            cache = DefaultYieldIncentivesCache(currentTimeMillis = { testScope.testScheduler.currentTime }),
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            isMerklIncentivesEnabled = isMerklIncentivesEnabled,
        )

    private fun incentivesResponse(lastUpdatedAt: String = TIMESTAMP) = YieldIncentivesResponse(
        chainId = CHAIN_ID,
        userAddress = ADDRESS,
        moduleAddress = null,
        rewards = emptyList(),
        lastUpdatedAt = lastUpdatedAt,
    )

    private fun <T : Any> badRequest(): ApiResponse<T> = errorResponse(
        ApiResponseError.HttpException(
            code = ApiResponseError.HttpException.Code.BAD_REQUEST,
            message = null,
            errorBody = null,
        ),
    )

    private fun <T : Any> networkError(): ApiResponse<T> = errorResponse(ApiResponseError.NetworkException())

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> errorResponse(cause: ApiResponseError): ApiResponse<T> =
        ApiResponse.Error(cause) as ApiResponse<T>

    private companion object {
        const val CHAIN_ID = 1
        const val ADDRESS = "0x742d35cc6634c0532925a3b844bc454e4438f44e"
        const val TOKEN = "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48"
        const val TIMESTAMP = "2026-07-31T10:30:00.000Z"
    }
}