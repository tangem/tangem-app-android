package com.tangem.domain.promo.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.promo.PromoRepository
import com.tangem.domain.promo.models.EnrollResult
import com.tangem.domain.promo.models.EnrolledTokenReward
import com.tangem.domain.promo.models.PromoCampaignId
import com.tangem.domain.promo.models.TokenReward
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
internal class EnrollPromoCampaignUseCaseTest {

    private val repository: PromoRepository = mockk()
    private val useCase = EnrollPromoCampaignUseCase(repository)

    private val campaign = PromoCampaignId.WhaleSwapCashback
    private val walletIds = listOf(UserWalletId("abcdef012345"))
    private val tokenReward = TokenReward("0xToken", "ethereum", "0xUser", "tether")
    private val resultTokenReward = EnrolledTokenReward("0xToken", "ethereum", "tether")

    @BeforeEach
    fun setUp() = clearMocks(repository)

    @Test
    fun `GIVEN repo returns Success WHEN invoke THEN Right Success`() = runTest {
        // Arrange
        val expected = EnrollResult.Success(resultTokenReward)
        coEvery { repository.enroll(campaign, tokenReward, walletIds) } returns expected

        // Act
        val result = useCase(campaign, tokenReward, walletIds)

        // Assert
        assertThat(result.getOrNull()).isEqualTo(expected)
    }

    @Test
    fun `GIVEN repo throws WHEN invoke THEN Left`() = runTest {
        // Arrange
        val error = IOException("x")
        coEvery { repository.enroll(campaign, tokenReward, walletIds) } throws error

        // Act
        val result = useCase(campaign, tokenReward, walletIds)

        // Assert
        assertEitherLeft(result, error)
    }
}