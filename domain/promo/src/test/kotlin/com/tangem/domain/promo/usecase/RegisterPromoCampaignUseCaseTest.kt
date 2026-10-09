package com.tangem.domain.promo.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.promo.PromoRepository
import com.tangem.domain.promo.models.PromoEnrollment
import com.tangem.domain.promo.models.PromoRegistrationError
import com.tangem.domain.promo.models.PromoRegistrationResult
import com.tangem.domain.promo.models.PromoRegistrationResult.PromoRegistrationStatus
import com.tangem.domain.promo.models.PromoWalletType
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class RegisterPromoCampaignUseCaseTest {

    private val repository: PromoRepository = mockk()
    private val useCase = RegisterPromoCampaignUseCase(repository)

    private val promotionId = 1
    private val userWalletId = UserWalletId("abcdef012345")
    private val enrollments = listOf(
        PromoEnrollment(
            userWalletId = userWalletId,
            walletType = PromoWalletType.Mobile,
            batchId = null,
            payout = null,
            isMain = true,
        ),
    )

    @BeforeEach
    fun setUp() = clearMocks(repository)

    @Test
    fun `GIVEN repo returns results WHEN invoke THEN Right with the same results`() = runTest {
        // Arrange
        val expected = listOf(PromoRegistrationResult(userWalletId, PromoRegistrationStatus.Enrolled, payout = null))
        coEvery { repository.register(promotionId, enrollments) } returns expected.right()

        // Act
        val actual = useCase(promotionId, enrollments)

        // Assert
        assertThat(actual).isEqualTo(expected.right())
    }

    @Test
    fun `GIVEN repo returns error WHEN invoke THEN Left with the same error`() = runTest {
        // Arrange
        coEvery { repository.register(promotionId, enrollments) } returns
            PromoRegistrationError.CampaignNotActive.left()

        // Act
        val actual = useCase(promotionId, enrollments)

        // Assert
        assertThat(actual).isEqualTo(PromoRegistrationError.CampaignNotActive.left())
    }
}