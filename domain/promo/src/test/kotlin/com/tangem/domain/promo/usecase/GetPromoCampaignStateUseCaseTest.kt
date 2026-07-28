package com.tangem.domain.promo.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.promo.PromoRepository
import com.tangem.domain.promo.models.PromoCampaignId
import com.tangem.domain.promo.models.PromoCampaignState
import com.tangem.test.core.assertEitherLeft
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.IOException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetPromoCampaignStateUseCaseTest {

    private val repository: PromoRepository = mockk()
    private val useCase = GetPromoCampaignStateUseCase(repository)

    private val campaign = PromoCampaignId.WhaleSwapCashback
    private val userWalletId = UserWalletId("abcdef012345")

    @BeforeEach
    fun setUp() = clearMocks(repository)

    @Test
    fun `GIVEN repo returns state WHEN invoke THEN Right of state`() = runTest {
        // Arrange
        val expected = PromoCampaignState.NotActive(campaign)
        coEvery { repository.getCampaignState(campaign, userWalletId, false) } returns expected

        // Act
        val result = useCase(campaign, userWalletId)

        // Assert
        assertThat(result.getOrNull()).isEqualTo(expected)
    }

    @Test
    fun `GIVEN repo throws WHEN invoke THEN Left`() = runTest {
        // Arrange
        val error = IOException("x")
        coEvery { repository.getCampaignState(campaign, userWalletId, false) } throws error

        // Act
        val result = useCase(campaign, userWalletId)

        // Assert
        assertEitherLeft(result, error)
    }
}