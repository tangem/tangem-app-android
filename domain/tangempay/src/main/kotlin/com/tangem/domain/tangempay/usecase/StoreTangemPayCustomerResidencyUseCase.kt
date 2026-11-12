package com.tangem.domain.tangempay.usecase

import com.tangem.domain.models.tangempay.TangemPayCustomerResidency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.tangempay.repository.OnboardingRepository

class StoreTangemPayCustomerResidencyUseCase(
    private val onboardingRepository: OnboardingRepository,
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
) {
    suspend operator fun invoke(userWalletId: UserWalletId, residency: TangemPayCustomerResidency) {
        onboardingRepository.storeCustomerResidency(userWalletId = userWalletId, residency = residency)
        paymentAccountStatusFetcher(userWalletId)
    }
}