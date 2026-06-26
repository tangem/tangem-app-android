package com.tangem.domain.marketing

import com.google.common.truth.Truth.assertThat
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DismissMarketingBannerUseCaseTest {

    private val repository: MarketingRepository = mockk()
    private val useCase = DismissMarketingBannerUseCase(repository)

    @BeforeEach
    fun reset() = clearMocks(repository)

    @Test
    fun `GIVEN campaign id WHEN invoke THEN repository dismiss called and Right returned`() = runTest {
        // Arrange
        coEvery { repository.dismissBanner(7) } returns Unit

        // Act
        val result = useCase(7)

        // Assert
        assertThat(result.isRight()).isTrue()
        coVerify(exactly = 1) { repository.dismissBanner(7) }
    }
}