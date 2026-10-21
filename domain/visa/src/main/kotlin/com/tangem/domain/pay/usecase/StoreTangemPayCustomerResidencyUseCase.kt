package com.tangem.domain.pay.usecase

import com.tangem.domain.models.pay.TangemPayCustomerResidency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.repository.OnboardingRepository

class StoreTangemPayCustomerResidencyUseCase(
    private val onboardingRepository: OnboardingRepository,
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
) {
    suspend operator fun invoke(userWalletId: UserWalletId, residency: TangemPayCustomerResidency) {
        onboardingRepository.storeCustomerResidency(userWalletId = userWalletId, residency = residency)
        paymentAccountStatusFetcher(userWalletId)
    }
}