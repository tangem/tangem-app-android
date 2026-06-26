package com.tangem.data.marketing

import com.google.common.truth.Truth.assertThat
import com.tangem.data.marketing.converter.MarketingCampaignConverter
import com.tangem.data.marketing.store.MarketingCampaignsCacheStore
import com.tangem.data.marketing.store.MarketingDismissStore
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.common.response.ApiResponseError.HttpException.Code
import com.tangem.datasource.api.common.response.ETAG_HEADER
import com.tangem.datasource.api.marketing.MarketingApi
import com.tangem.datasource.api.marketing.models.BannerDto
import com.tangem.datasource.api.marketing.models.CampaignDto
import com.tangem.datasource.api.marketing.models.MarketingCampaignsCacheEntry
import com.tangem.datasource.api.marketing.models.MarketingCampaignsResponse
import com.tangem.domain.marketing.models.MarketingScreen
import com.tangem.utils.SupportedLanguages
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
internal class DefaultMarketingRepositoryTest {

    private val marketingApi: MarketingApi = mockk()
    private val cacheStore: MarketingCampaignsCacheStore = mockk(relaxed = true)
    private val dismissStore: MarketingDismissStore = mockk(relaxed = true)

    private val language = SupportedLanguages.getCurrentSupportedLanguageCode()

    private val repository = DefaultMarketingRepository(
        marketingApi = marketingApi,
        cacheStore = cacheStore,
        dismissStore = dismissStore,
        converter = MarketingCampaignConverter(),
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
    fun reset() {
        clearMocks(marketingApi, cacheStore, dismissStore)
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
        coEvery { marketingApi.getCampaigns(type = "token_details", language = language, eTag = null) } returns
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
        coEvery { marketingApi.getCampaigns(type = "token_details", language = language, eTag = "etag") } returns
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
        coEvery { marketingApi.getCampaigns(type = "staking", language = language, eTag = "etag") } returns
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
        coEvery { marketingApi.getCampaigns(type = "staking", language = language, eTag = null) } returns
            httpError(Code.INTERNAL_SERVER_ERROR)

        // Act
        val result = repository.getCampaigns(MarketingScreen.Staking(networkId = "ethereum", contractAddress = "0x"))

        // Assert
        assertThat(result.getOrNull()).isEmpty()
    }

    @Test
    fun `GIVEN swap screen WHEN getCampaigns THEN sends pair params and does not touch cache`() = runTest {
        // Arrange
        coEvery {
            marketingApi.getCampaigns(
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
    fun `GIVEN dismiss WHEN dismissBanner THEN delegates to dismiss store`() = runTest {
        // Act
        repository.dismissBanner(42)

        // Assert
        coVerify(exactly = 1) { dismissStore.dismiss(42) }
    }
}