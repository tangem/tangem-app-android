package com.tangem.domain.pay.usecase

import arrow.core.right
import com.tangem.domain.models.pay.TangemPayCustomerResidency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.repository.OnboardingRepository
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class StoreTangemPayCustomerResidencyUseCaseTest {

    private val onboardingRepository: OnboardingRepository = mockk(relaxUnitFun = true)
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher = mockk()

    private val useCase = StoreTangemPayCustomerResidencyUseCase(
        onboardingRepository = onboardingRepository,
        paymentAccountStatusFetcher = paymentAccountStatusFetcher,
    )

    private val userWalletId = UserWalletId("1234567890ABCDEF")
    private val residency = TangemPayCustomerResidency(countryCode = "US", regionCode = "TX")

    @Test
    fun `GIVEN customer residency WHEN invoke THEN residency stored before status refreshed`() = runTest {
        // Arrange
        coEvery { paymentAccountStatusFetcher(userWalletId) } returns Unit.right()

        // Act
        useCase(userWalletId = userWalletId, residency = residency)

        // Assert
        coVerifyOrder {
            onboardingRepository.storeCustomerResidency(userWalletId, residency)
            paymentAccountStatusFetcher(userWalletId)
        }
    }
}