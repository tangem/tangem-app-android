package com.tangem.domain.pay.usecase

import arrow.core.right
import com.tangem.domain.models.pay.TangemPayCustomerResidency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.repository.OnboardingRepository
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ClearTangemPayCustomerResidencyUseCaseTest {

    private val onboardingRepository: OnboardingRepository = mockk(relaxUnitFun = true)
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher = mockk()

    private val useCase = ClearTangemPayCustomerResidencyUseCase(
        onboardingRepository = onboardingRepository,
        paymentAccountStatusFetcher = paymentAccountStatusFetcher,
    )

    private val userWalletId = UserWalletId("1234567890ABCDEF")

    @BeforeEach
    fun resetMocks() {
        clearMocks(onboardingRepository, paymentAccountStatusFetcher)
        coEvery { paymentAccountStatusFetcher(userWalletId) } returns Unit.right()
    }

    @Test
    fun `GIVEN stored residency WHEN invoke THEN residency cleared before status refreshed`() = runTest {
        // Arrange
        coEvery { onboardingRepository.getCustomerResidency(userWalletId) } returns
            TangemPayCustomerResidency(countryCode = "US", regionCode = "TX")

        // Act
        useCase(userWalletId)

        // Assert
        coVerifyOrder {
            onboardingRepository.clearCustomerResidency(userWalletId)
            paymentAccountStatusFetcher(userWalletId)
        }
    }

    @Test
    fun `GIVEN no stored residency WHEN invoke THEN nothing cleared AND status not refreshed`() = runTest {
        // Arrange
        coEvery { onboardingRepository.getCustomerResidency(userWalletId) } returns null

        // Act
        useCase(userWalletId)

        // Assert
        coVerify(exactly = 0) { onboardingRepository.clearCustomerResidency(any()) }
        coVerify(exactly = 0) { paymentAccountStatusFetcher(any<UserWalletId>()) }
    }
}