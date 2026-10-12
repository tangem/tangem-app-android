package com.tangem.domain.pay.usecase

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.repository.OnboardingRepository
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class DismissVirtualAccountOrderFailureUseCaseTest {

    private val onboardingRepository: OnboardingRepository = mockk(relaxUnitFun = true)
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher = mockk(relaxUnitFun = true)

    private val useCase = DismissVirtualAccountOrderFailureUseCase(
        onboardingRepository = onboardingRepository,
        paymentAccountStatusFetcher = paymentAccountStatusFetcher,
    )

    private val userWalletId = UserWalletId("1234567890ABCDEF")

    @Test
    fun `GIVEN failed banking details order WHEN invoke THEN failure cleared before the cached onramp flips`() =
        runTest {
            // Act
            useCase(userWalletId)

            // Assert
            coVerifyOrder {
                onboardingRepository.storeVirtualAccountOrderFailed(userWalletId, false)
                paymentAccountStatusFetcher.markVirtualAccountEligible(userWalletId)
            }
        }
}