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
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

/**
 * The incentives are an overlay on a Yield Mode that has to keep working without them, so every endpoint answers
 * with an error value and never with a throw: the callers run inside model scopes that have no exception handler,
 * and a throw there takes the screen — or the process — down with it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultYieldIncentivesRepositoryIsolationTest {

    private val api: YieldIncentivesApi = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(api)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN the api throws WHEN an endpoint is called THEN unavailable instead of throwing`(
        model: EndpointModel,
    ) = runTest {
        // Arrange
        val repository = createRepository(testScope = this)
        model.stubThrow(api)

        // Act
        val actual = model.call(repository)

        // Assert
        assertThat(actual).isEqualTo(YieldIncentivesError.Unavailable.left())
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a transport failure WHEN an endpoint is called THEN unavailable`(model: EndpointModel) = runTest {
        // Arrange
        val repository = createRepository(testScope = this)
        model.stubResponse(api, ApiResponseError.NetworkException())

        // Act
        val actual = model.call(repository)

        // Assert
        assertThat(actual).isEqualTo(YieldIncentivesError.Unavailable.left())
    }

    /** Only a 400 means the chain carries no incentives; every other status is a failure to report as such. */
    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a server error WHEN an endpoint is called THEN unavailable`(model: EndpointModel) = runTest {
        // Arrange
        val repository = createRepository(testScope = this)
        model.stubResponse(api, httpError(ApiResponseError.HttpException.Code.INTERNAL_SERVER_ERROR))

        // Act
        val actual = model.call(repository)

        // Assert
        assertThat(actual).isEqualTo(YieldIncentivesError.Unavailable.left())
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN an unsupported chain WHEN an endpoint is called THEN the feature reads as absent`(
        model: EndpointModel,
    ) = runTest {
        // Arrange
        val repository = createRepository(testScope = this)
        model.stubResponse(api, httpError(ApiResponseError.HttpException.Code.BAD_REQUEST))

        // Act
        val actual = model.call(repository)

        // Assert
        assertThat(actual).isEqualTo(YieldIncentivesError.FeatureAbsent.left())
    }

    internal data class EndpointModel(
        val name: String,
        val stubThrow: (YieldIncentivesApi) -> Unit,
        val stubResponse: (YieldIncentivesApi, ApiResponseError) -> Unit,
        val call: suspend (YieldIncentivesRepository) -> Either<YieldIncentivesError, Any>,
    ) {
        override fun toString(): String = name
    }

    private fun provideTestModels() = listOf(
        EndpointModel(
            name = "getIncentives",
            stubThrow = { api ->
                coEvery { api.getIncentives(chainId = CHAIN_ID, userAddress = ADDRESS) } throws FAILURE
            },
            stubResponse = { api, cause ->
                coEvery { api.getIncentives(chainId = CHAIN_ID, userAddress = ADDRESS) } returns errorResponse(cause)
            },
            call = { it.getIncentives(chainId = CHAIN_ID, userAddress = ADDRESS) },
        ),
        EndpointModel(
            name = "getAprHistory",
            stubThrow = { api ->
                coEvery { api.getAprHistory(chainId = CHAIN_ID, tokenAddress = TOKEN, days = DAYS) } throws FAILURE
            },
            stubResponse = { api, cause ->
                coEvery {
                    api.getAprHistory(chainId = CHAIN_ID, tokenAddress = TOKEN, days = DAYS)
                } returns errorResponse(cause)
            },
            call = { it.getAprHistory(chainId = CHAIN_ID, tokenAddress = TOKEN, days = DAYS) },
        ),
        EndpointModel(
            name = "getClaimCalldata",
            stubThrow = { api ->
                coEvery { api.getClaimCalldata(chainId = CHAIN_ID, userAddress = ADDRESS) } throws FAILURE
            },
            stubResponse = { api, cause ->
                coEvery {
                    api.getClaimCalldata(chainId = CHAIN_ID, userAddress = ADDRESS)
                } returns errorResponse(cause)
            },
            call = { it.getClaimCalldata(chainId = CHAIN_ID, userAddress = ADDRESS) },
        ),
        EndpointModel(
            name = "getClaimHistory",
            stubThrow = { api ->
                coEvery { api.getClaimHistory(chainId = CHAIN_ID, userAddress = ADDRESS) } throws FAILURE
            },
            stubResponse = { api, cause ->
                coEvery {
                    api.getClaimHistory(chainId = CHAIN_ID, userAddress = ADDRESS)
                } returns errorResponse(cause)
            },
            call = { it.getClaimHistory(chainId = CHAIN_ID, userAddress = ADDRESS) },
        ),
    )

    private fun createRepository(testScope: TestScope) = DefaultYieldIncentivesRepository(
        api = api,
        cache = DefaultYieldIncentivesCache(currentTimeMillis = { testScope.testScheduler.currentTime }),
        dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        isMerklIncentivesEnabled = true,
    )

    private fun httpError(code: ApiResponseError.HttpException.Code) =
        ApiResponseError.HttpException(code = code, message = null, errorBody = null)

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> errorResponse(cause: ApiResponseError): ApiResponse<T> =
        ApiResponse.Error(cause) as ApiResponse<T>

    private companion object {
        const val CHAIN_ID = 1
        const val DAYS = 30
        const val ADDRESS = "0x742d35cc6634c0532925a3b844bc454e4438f44e"
        const val TOKEN = "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48"
        val FAILURE = IllegalStateException("the call itself blew up")
    }
}