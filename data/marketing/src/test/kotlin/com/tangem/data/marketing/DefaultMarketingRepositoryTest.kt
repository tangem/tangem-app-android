package com.tangem.data.marketing

import com.google.common.truth.Truth.assertThat
import com.tangem.data.marketing.converter.MarketingCampaignConverter
import com.tangem.data.marketing.store.MarketingCampaignsCacheStore
import com.tangem.data.marketing.store.MarketingDismissStore
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ApiResponseError.HttpException.Code
import com.tangem.datasource.api.common.response.ETAG_HEADER
import com.tangem.datasource.api.marketing.models.BannerDto
import com.tangem.datasource.api.marketing.models.CampaignDto
import com.tangem.datasource.api.marketing.models.MarketingCampaignsCacheEntry
import com.tangem.datasource.api.marketing.models.MarketingCampaignsResponse
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.domain.marketing.models.MarketingScreen
import com.tangem.domain.marketing.models.MarketingScreenType
import com.tangem.utils.SupportedLanguages
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultMarketingRepositoryTest {

    private val tangemTechApi: TangemTechApi = mockk()
    private val cacheStore: MarketingCampaignsCacheStore = mockk(relaxed = true)
    private val dismissStore: MarketingDismissStore = mockk(relaxed = true)

    private val language = SupportedLanguages.getCurrentSupportedLanguageCode()

    // Recreated per test (not a val): DefaultMarketingRepository now holds mutable in-memory session-cache
    // state, which would otherwise leak between tests sharing this PER_CLASS instance.
    private lateinit var repository: DefaultMarketingRepository

    @BeforeEach
    fun reset() {
        clearMocks(tangemTechApi, cacheStore, dismissStore)
        repository = DefaultMarketingRepository(
            tangemTechApi = tangemTechApi,
            cacheStore = cacheStore,
            dismissStore = dismissStore,
            converter = MarketingCampaignConverter(),
            dispatchers = TestingCoroutineDispatcherProvider(),
        )
    }

    private fun response(id: Int) = MarketingCampaignsResponse(
        campaigns = listOf(CampaignDto(id = id, type = "token_details", priority = 1, banner = BannerDto(uiType = "standalone"))),
    )

    @Suppress("UNCHECKED_CAST")
    private fun httpError(code: Code): ApiResponse<MarketingCampaignsResponse> = ApiResponse.Error(
        cause = ApiResponseError.HttpException(code = code, message = null, errorBody = null),
    ) as ApiResponse<MarketingCampaignsResponse>

    @Test
    fun `GIVEN 200 for background type WHEN getCampaigns THEN stores etag and returns campaigns`() = runTest {
        // Arrange
        coEvery { cacheStore.get("token_details") } returns null
        coEvery { tangemTechApi.getMarketingCampaigns(type = "token_details", language = language, eTag = null) } returns
            ApiResponse.Success(data = response(id = 7), headers = mapOf(ETAG_HEADER to listOf("new-etag")))

        // Act
        val result = repository.getCampaigns(MarketingScreen.TokenDetails(networkId = "ethereum", contractAddress = "0x"))

        // Assert
        assertThat(result.getOrNull()!!.map { it.id }).containsExactly(7)
        coVerify(exactly = 1) {
            cacheStore.store("token_details", MarketingCampaignsCacheEntry(eTag = "new-etag", response = response(id = 7)))
        }
    }

    @Test
    fun `GIVEN 304 for background type WHEN getCampaigns THEN returns cached campaigns`() = runTest {
        // Arrange
        coEvery { cacheStore.get("token_details") } returns
            MarketingCampaignsCacheEntry(eTag = "etag", response = response(id = 9))
        coEvery { tangemTechApi.getMarketingCampaigns(type = "token_details", language = language, eTag = "etag") } returns
            httpError(Code.NOT_MODIFIED)

        // Act
        val result = repository.getCampaigns(MarketingScreen.TokenDetails(networkId = "ethereum", contractAddress = "0x"))

        // Assert
        assertThat(result.getOrNull()!!.map { it.id }).containsExactly(9)
        coVerify(exactly = 0) { cacheStore.store(any(), any()) }
    }

    @Test
    fun `GIVEN 5xx with cache WHEN getCampaigns THEN returns cached`() = runTest {
        // Arrange
        coEvery { cacheStore.get("staking") } returns
            MarketingCampaignsCacheEntry(eTag = "etag", response = response(id = 5))
        coEvery { tangemTechApi.getMarketingCampaigns(type = "staking", language = language, eTag = "etag") } returns
            httpError(Code.SERVICE_UNAVAILABLE)

        // Act
        val result = repository.getCampaigns(MarketingScreen.Staking(networkId = "ethereum", contractAddress = "0x"))

        // Assert
        assertThat(result.getOrNull()!!.map { it.id }).containsExactly(5)
    }

    @Test
    fun `GIVEN 5xx without cache WHEN getCampaigns THEN returns empty`() = runTest {
        // Arrange
        coEvery { cacheStore.get("staking") } returns null
        coEvery { tangemTechApi.getMarketingCampaigns(type = "staking", language = language, eTag = null) } returns
            httpError(Code.INTERNAL_SERVER_ERROR)

        // Act
        val result = repository.getCampaigns(MarketingScreen.Staking(networkId = "ethereum", contractAddress = "0x"))

        // Assert
        assertThat(result.getOrNull()).isEmpty()
    }

    @Test
    fun `GIVEN 5xx without cache WHEN getCampaigns twice THEN not session-cached and retried`() = runTest {
        // Arrange
        coEvery { cacheStore.get("staking") } returns null
        coEvery { tangemTechApi.getMarketingCampaigns(type = "staking", language = language, eTag = null) } returns
            httpError(Code.SERVICE_UNAVAILABLE)
        val screen = MarketingScreen.Staking(networkId = "ethereum", contractAddress = "0x")

        // Act
        val first = repository.getCampaigns(screen)
        val second = repository.getCampaigns(screen)

        // Assert
        assertThat(first.getOrNull()).isEmpty()
        assertThat(second.getOrNull()).isEmpty()
        coVerify(exactly = 2) { tangemTechApi.getMarketingCampaigns(type = "staking", language = language, eTag = null) }
    }

    @Test
    fun `GIVEN swap screen WHEN getCampaigns THEN sends pair params and does not touch cache`() = runTest {
        // Arrange
        coEvery {
            tangemTechApi.getMarketingCampaigns(
                type = "swap", language = language,
                fromNetwork = "ethereum", fromContractAddress = "0xFrom",
                toNetwork = "bitcoin", toContractAddress = "0xTo",
            )
        } returns ApiResponse.Success(data = response(id = 3))

        // Act
        val result = repository.getCampaigns(
            MarketingScreen.Swap(
                fromNetwork = "ethereum", fromContractAddress = "0xFrom",
                toNetwork = "bitcoin", toContractAddress = "0xTo",
            ),
        )

        // Assert
        assertThat(result.getOrNull()!!.map { it.id }).containsExactly(3)
        coVerify(exactly = 0) { cacheStore.get(any()) }
        coVerify(exactly = 0) { cacheStore.store(any(), any()) }
    }

    @Test
    fun `GIVEN cached in session WHEN getCampaigns twice THEN api called once`() = runTest {
        // Arrange
        coEvery { cacheStore.get("token_details") } returns null
        coEvery { tangemTechApi.getMarketingCampaigns(type = "token_details", language = language, eTag = null) } returns
            ApiResponse.Success(data = response(id = 7))

        // Act
        val screen = MarketingScreen.TokenDetails(networkId = "ethereum", contractAddress = "0x")
        val first = repository.getCampaigns(screen)
        val second = repository.getCampaigns(screen)

        // Assert
        assertThat(first.getOrNull()!!.map { it.id }).containsExactly(7)
        assertThat(second.getOrNull()!!.map { it.id }).containsExactly(7)
        coVerify(exactly = 1) { tangemTechApi.getMarketingCampaigns(type = "token_details", language = language, eTag = null) }
    }

    @Test
    fun `GIVEN two concurrent getCampaigns for same type WHEN both in flight THEN api called once`() = runTest {
        // Arrange
        val gate = CompletableDeferred<Unit>()
        coEvery { cacheStore.get("token_details") } returns null
        coEvery { tangemTechApi.getMarketingCampaigns(type = "token_details", language = language, eTag = null) } coAnswers {
            gate.await() // first caller suspends inside the lock, second blocks on the mutex
            ApiResponse.Success(data = response(id = 7))
        }
        val screen = MarketingScreen.TokenDetails(networkId = "ethereum", contractAddress = "0x")

        // Act — launch both before either completes, then release the API
        val a = async { repository.getCampaigns(screen) }
        val b = async { repository.getCampaigns(screen) }
        runCurrent()
        gate.complete(Unit)
        val first = a.await()
        val second = b.await()

        // Assert
        assertThat(first.getOrNull()!!.map { it.id }).containsExactly(7)
        assertThat(second.getOrNull()!!.map { it.id }).containsExactly(7)
        coVerify(exactly = 1) { tangemTechApi.getMarketingCampaigns(type = "token_details", language = language, eTag = null) }
    }

    @Test
    fun `GIVEN prefetch WHEN getCampaigns THEN served from session cache without extra api call`() = runTest {
        // Arrange
        coEvery { cacheStore.get("markets_token") } returns null
        coEvery { tangemTechApi.getMarketingCampaigns(type = "markets_token", language = language, eTag = null) } returns
            ApiResponse.Success(data = response(id = 3))

        // Act
        repository.prefetchBackgroundCampaigns(MarketingScreenType.TOKEN_MARKETS)
        val result = repository.getCampaigns(MarketingScreen.TokenMarkets(coingeckoId = "id"))

        // Assert
        assertThat(result.getOrNull()!!.map { it.id }).containsExactly(3)
        coVerify(exactly = 1) { tangemTechApi.getMarketingCampaigns(type = "markets_token", language = language, eTag = null) }
    }

    @Test
    fun `GIVEN swap WHEN getCampaigns twice THEN never session-cached (api called each time)`() = runTest {
        // Arrange
        val swap = MarketingScreen.Swap("eth", "0xF", "btc", "0xT")
        coEvery {
            tangemTechApi.getMarketingCampaigns(
                type = "swap", language = language,
                fromNetwork = "eth", fromContractAddress = "0xF", toNetwork = "btc", toContractAddress = "0xT",
            )
        } returns ApiResponse.Success(data = response(id = 1))

        // Act
        repository.getCampaigns(swap)
        repository.getCampaigns(swap)

        // Assert
        coVerify(exactly = 2) {
            tangemTechApi.getMarketingCampaigns(
                type = "swap", language = language,
                fromNetwork = "eth", fromContractAddress = "0xF", toNetwork = "btc", toContractAddress = "0xT",
            )
        }
    }

    @Test
    fun `GIVEN dismiss WHEN dismissBanner THEN delegates to dismiss store`() = runTest {
        // Act
        repository.dismissBanner(42)

        // Assert
        coVerify(exactly = 1) { dismissStore.dismiss(42) }
    }
}