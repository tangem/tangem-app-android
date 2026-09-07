package com.tangem.domain.sociallogin.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.sociallogin.SocialLoginFeatureToggles
import com.tangem.domain.sociallogin.models.SocialLoginProvider
import com.tangem.domain.sociallogin.repository.SocialLoginProviderRepository
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class GetAvailableSocialLoginProvidersUseCaseTest {

    private val socialLoginProviderRepository: SocialLoginProviderRepository = mockk()
    private val socialLoginFeatureToggles: SocialLoginFeatureToggles = mockk()

    private val useCase = GetAvailableSocialLoginProvidersUseCase(
        socialLoginProviderRepository = socialLoginProviderRepository,
        socialLoginFeatureToggles = socialLoginFeatureToggles,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(socialLoginProviderRepository, socialLoginFeatureToggles)
    }

    @Test
    fun `GIVEN feature enabled WHEN invoke THEN device providers are returned`() {
        // Arrange
        every { socialLoginFeatureToggles.isSocialLoginEnabled } returns true
        every { socialLoginProviderRepository.getAvailableProviders() } returns listOf(SocialLoginProvider.Google)

        // Act
        val actual = useCase()

        // Assert
        assertThat(actual).containsExactly(SocialLoginProvider.Google)
    }

    @Test
    fun `GIVEN feature disabled WHEN invoke THEN no providers are returned`() {
        // Arrange
        every { socialLoginFeatureToggles.isSocialLoginEnabled } returns false

        // Act
        val actual = useCase()

        // Assert
        assertThat(actual).isEmpty()
        verify(exactly = 0) { socialLoginProviderRepository.getAvailableProviders() }
    }
}