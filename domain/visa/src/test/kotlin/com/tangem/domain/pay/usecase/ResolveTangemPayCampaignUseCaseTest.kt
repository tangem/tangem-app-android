package com.tangem.domain.pay.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.pay.model.TangemPayCampaign
import com.tangem.domain.pay.model.TangemPayCampaignResolution
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.pay.repository.TangemPayCampaignRepository
import com.tangem.domain.visa.error.VisaApiError
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ResolveTangemPayCampaignUseCaseTest {

    private val onboardingRepository: OnboardingRepository = mockk()
    private val campaignRepository: TangemPayCampaignRepository = mockk()

    private val useCase = ResolveTangemPayCampaignUseCase(
        onboardingRepository = onboardingRepository,
        campaignRepository = campaignRepository,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(onboardingRepository, campaignRepository)
        coEvery { onboardingRepository.validateDeeplink(CAMPAIGN_LINK) } returns true.right()
        coEvery { campaignRepository.getCampaign(CAMPAIGN_KEY) } returns CAMPAIGN.right()
    }

    @Test
    fun `GIVEN a valid campaign WHEN invoked THEN the link is validated AND landing is resolved`() = runTest {
        // Act
        val result = useCase(CAMPAIGN_KEY)

        // Assert
        assertThat(result.getOrNull()).isEqualTo(TangemPayCampaignResolution.Landing(CAMPAIGN))
        coVerify(exactly = 1) { onboardingRepository.validateDeeplink("tangem://marketing/$CAMPAIGN_KEY") }
    }

    @Test
    fun `GIVEN a campaign without art WHEN invoked THEN landing is resolved with no art`() = runTest {
        // Arrange
        coEvery { campaignRepository.getCampaign(CAMPAIGN_KEY) } returns TangemPayCampaign(cardArtUrl = null).right()

        // Act
        val result = useCase(CAMPAIGN_KEY)

        // Assert
        assertThat(result.getOrNull()).isEqualTo(
            TangemPayCampaignResolution.Landing(TangemPayCampaign(cardArtUrl = null)),
        )
    }

    @Test
    fun `GIVEN an invalid campaign WHEN invoked THEN unavailable is resolved AND campaign is not loaded`() = runTest {
        // Arrange
        coEvery { onboardingRepository.validateDeeplink(CAMPAIGN_LINK) } returns false.right()

        // Act
        val result = useCase(CAMPAIGN_KEY)

        // Assert
        assertThat(result.getOrNull()).isEqualTo(TangemPayCampaignResolution.Unavailable)
        coVerify(exactly = 0) { campaignRepository.getCampaign(any()) }
    }

    @Test
    fun `GIVEN validation fails WHEN invoked THEN the error is propagated AND campaign is not loaded`() = runTest {
        // Arrange
        coEvery { onboardingRepository.validateDeeplink(CAMPAIGN_LINK) } returns VisaApiError.ServerUnavailable.left()

        // Act
        val result = useCase(CAMPAIGN_KEY)

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(VisaApiError.ServerUnavailable)
        coVerify(exactly = 0) { campaignRepository.getCampaign(any()) }
    }

    @Test
    fun `GIVEN the campaign request fails WHEN invoked THEN the error is propagated`() = runTest {
        // Arrange
        coEvery { campaignRepository.getCampaign(CAMPAIGN_KEY) } returns VisaApiError.Unspecified.left()

        // Act
        val result = useCase(CAMPAIGN_KEY)

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(VisaApiError.Unspecified)
    }

    private companion object {
        const val CAMPAIGN_KEY = "tpay_for_oxford"
        const val CAMPAIGN_LINK = "tangem://marketing/tpay_for_oxford"
        val CAMPAIGN = TangemPayCampaign(cardArtUrl = "https://example.com/oxford.png")
    }
}