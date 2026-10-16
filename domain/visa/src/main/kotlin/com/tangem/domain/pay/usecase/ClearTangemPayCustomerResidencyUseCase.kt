package com.tangem.domain.pay.usecase

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.repository.OnboardingRepository

class ClearTangemPayCustomerResidencyUseCase(
    private val onboardingRepository: OnboardingRepository,
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
) {
    suspend operator fun invoke(userWalletId: UserWalletId) {
        if (onboardingRepository.getCustomerResidency(userWalletId) == null) return

        onboardingRepository.clearCustomerResidency(userWalletId)
        paymentAccountStatusFetcher(userWalletId)
    }
}