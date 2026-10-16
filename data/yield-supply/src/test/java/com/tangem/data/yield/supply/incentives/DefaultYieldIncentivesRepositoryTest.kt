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
import org.junit.jupiter.api.Nested
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
    fun `GIVEN unparseable timestamp WHEN getIncentives THEN the rewards still arrive`() = runTest {
        // Arrange - nothing reads the stamp, so letting it sink a payload of real rewards would cost the owner
        // their claim over a field no screen shows.
        val repository = createRepository(testScope = this)
        coEvery { api.getIncentives(chainId = CHAIN_ID, userAddress = ADDRESS) } returns
            success(incentivesResponse(lastUpdatedAt = "yesterday"))

        // Act
        val actual = repository.getIncentives(chainId = CHAIN_ID, userAddress = ADDRESS)

        // Assert
        assertThat(actual.getOrNull()?.lastUpdatedAt).isNull()
        assertThat(actual.isRight()).isTrue()
    }

    @Test
    fun `GIVEN claimRecommended absent WHEN getIncentives THEN unavailable instead of a silent verdict`() = runTest {
        // Arrange — reading a missing key as "not recommended" would leave the Claim action dark with nothing
        // on screen to say why, so the overlay has to fail visibly instead.
        val repository = createRepository(testScope = this)
        coEvery { api.getIncentives(chainId = CHAIN_ID, userAddress = ADDRESS) } returns
            success(incentivesResponse().copy(isClaimRecommended = null))

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
            success(incentivesResponse())

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
            success(incentivesResponse())

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
            success(incentivesResponse())
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
            success(YieldClaimCalldataResponse(chainId = CHAIN_ID, hasClaimable = false))

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
            success(
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
            success(YieldAprHistoryResponse(chainId = CHAIN_ID, tokenAddress = TOKEN, points = null))

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
            success(YieldAprHistoryResponse(chainId = CHAIN_ID, tokenAddress = TOKEN, points = null))

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
            success(YieldClaimHistoryResponse(chainId = CHAIN_ID, claims = null))

        // Act
        repository.getClaimHistory(chainId = CHAIN_ID, userAddress = ADDRESS)
        advanceTimeBy(delayTimeMillis = 59_000)
        repository.getClaimHistory(chainId = CHAIN_ID, userAddress = ADDRESS)

        // Assert
        coVerify(exactly = 1) { api.getClaimHistory(chainId = CHAIN_ID, userAddress = ADDRESS) }
    }

    @Test
    fun `GIVEN cached user data WHEN invalidateUserCache THEN both endpoints are fetched again`() = runTest {
        // Arrange
        val repository = createRepository(testScope = this)
        coEvery { api.getIncentives(chainId = CHAIN_ID, userAddress = ADDRESS) } returns
            success(incentivesResponse())
        coEvery { api.getClaimHistory(chainId = CHAIN_ID, userAddress = ADDRESS) } returns
            success(YieldClaimHistoryResponse(chainId = CHAIN_ID, claims = null))
        repository.getIncentives(chainId = CHAIN_ID, userAddress = ADDRESS)
        repository.getClaimHistory(chainId = CHAIN_ID, userAddress = ADDRESS)

        // Act
        repository.invalidateUserCache(chainId = CHAIN_ID, userAddress = ADDRESS.uppercase())
        repository.getIncentives(chainId = CHAIN_ID, userAddress = ADDRESS)
        repository.getClaimHistory(chainId = CHAIN_ID, userAddress = ADDRESS)

        // Assert
        coVerify(exactly = 2) { api.getIncentives(chainId = CHAIN_ID, userAddress = ADDRESS) }
        coVerify(exactly = 2) { api.getClaimHistory(chainId = CHAIN_ID, userAddress = ADDRESS) }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ErrorResponses {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN an error response WHEN an endpoint is called THEN it maps to the domain error`(model: ErrorModel) =
            runTest {
                // Arrange
                val repository = createRepository(testScope = this)
                model.endpoint.stub(api, model.cause)

                // Act
                val actual = model.endpoint.call(repository)

                // Assert
                assertThat(actual).isEqualTo(model.expected.left())
            }

        private fun provideTestModels() = endpoints().flatMap { endpoint ->
            listOf(
                ErrorModel(endpoint, ApiResponseError.NetworkException(), YieldIncentivesError.Unavailable),
                ErrorModel(
                    endpoint = endpoint,
                    cause = httpError(ApiResponseError.HttpException.Code.INTERNAL_SERVER_ERROR),
                    expected = YieldIncentivesError.Unavailable,
                ),
                ErrorModel(
                    endpoint = endpoint,
                    cause = httpError(ApiResponseError.HttpException.Code.BAD_REQUEST),
                    expected = YieldIncentivesError.FeatureAbsent,
                ),
            )
        }

        private fun endpoints() = listOf(
            Endpoint(
                name = "getIncentives",
                stub = { api, cause ->
                    coEvery {
                        api.getIncentives(chainId = CHAIN_ID, userAddress = ADDRESS)
                    } returns errorResponse(cause)
                },
                call = { it.getIncentives(chainId = CHAIN_ID, userAddress = ADDRESS) },
            ),
            Endpoint(
                name = "getAprHistory",
                stub = { api, cause ->
                    coEvery {
                        api.getAprHistory(chainId = CHAIN_ID, tokenAddress = TOKEN, days = 30)
                    } returns errorResponse(cause)
                },
                call = { it.getAprHistory(chainId = CHAIN_ID, tokenAddress = TOKEN, days = 30) },
            ),
            Endpoint(
                name = "getClaimCalldata",
                stub = { api, cause ->
                    coEvery {
                        api.getClaimCalldata(chainId = CHAIN_ID, userAddress = ADDRESS)
                    } returns errorResponse(cause)
                },
                call = { it.getClaimCalldata(chainId = CHAIN_ID, userAddress = ADDRESS) },
            ),
            Endpoint(
                name = "getClaimHistory",
                stub = { api, cause ->
                    coEvery {
                        api.getClaimHistory(chainId = CHAIN_ID, userAddress = ADDRESS)
                    } returns errorResponse(cause)
                },
                call = { it.getClaimHistory(chainId = CHAIN_ID, userAddress = ADDRESS) },
            ),
        )

        private fun httpError(code: ApiResponseError.HttpException.Code) =
            ApiResponseError.HttpException(code = code, message = null, errorBody = null)
    }

    internal data class Endpoint(
        val name: String,
        val stub: (YieldIncentivesApi, ApiResponseError) -> Unit,
        val call: suspend (YieldIncentivesRepository) -> Either<YieldIncentivesError, Any>,
    ) {
        override fun toString(): String = name
    }

    internal data class ErrorModel(
        val endpoint: Endpoint,
        val cause: ApiResponseError,
        val expected: YieldIncentivesError,
    )

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
        isClaimRecommended = false,
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

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> errorResponse(cause: ApiResponseError): ApiResponse<T> =
        ApiResponse.Error(cause) as ApiResponse<T>

    private companion object {
        const val CHAIN_ID = 1
        const val ADDRESS = "0x742d35cc6634c0532925a3b844bc454e4438f44e"
        const val TOKEN = "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48"
        const val TIMESTAMP = "2026-07-31T10:30:00.000Z"
    }

    private fun <T : Any> success(payload: T) = ApiResponse.Success(payload)
}