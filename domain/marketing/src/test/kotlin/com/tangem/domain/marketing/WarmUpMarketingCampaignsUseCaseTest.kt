package com.tangem.domain.marketing

import com.tangem.domain.marketing.models.MarketingScreenType
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class WarmUpMarketingCampaignsUseCaseTest {

    private val repository: MarketingRepository = mockk(relaxed = true)
    private val featureToggles: MarketingFeatureToggles = mockk()
    private val useCase = WarmUpMarketingCampaignsUseCase(repository, featureToggles)

    @BeforeEach
    fun reset() = clearMocks(repository, featureToggles)

    @Test
    fun `GIVEN toggle off WHEN invoke THEN no prefetch`() = runTest {
        // Arrange
        every { featureToggles.isMarketingBannersEnabled } returns false

        // Act
        useCase()

        // Assert
        coVerify(exactly = 0) { repository.prefetchBackgroundCampaigns(any()) }
    }

    @Test
    fun `GIVEN toggle on WHEN invoke THEN prefetch token_details and markets`() = runTest {
        // Arrange
        every { featureToggles.isMarketingBannersEnabled } returns true

        // Act
        useCase()

        // Assert
        coVerify(exactly = 1) { repository.prefetchBackgroundCampaigns(MarketingScreenType.TOKEN_DETAILS) }
        coVerify(exactly = 1) { repository.prefetchBackgroundCampaigns(MarketingScreenType.TOKEN_MARKETS) }
    }

    @Test
    fun `GIVEN prefetch throws WHEN invoke THEN swallowed`() = runTest {
        // Arrange
        every { featureToggles.isMarketingBannersEnabled } returns true
        coEvery { repository.prefetchBackgroundCampaigns(any()) } throws RuntimeException("boom")

        // Act + Assert (does not throw)
        useCase()
    }
}