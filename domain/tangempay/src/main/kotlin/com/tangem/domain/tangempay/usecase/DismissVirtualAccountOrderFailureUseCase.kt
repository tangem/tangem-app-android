package com.tangem.domain.tangempay.usecase

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.tangempay.repository.OnboardingRepository

class DismissVirtualAccountOrderFailureUseCase(
    private val onboardingRepository: OnboardingRepository,
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
) {
    suspend operator fun invoke(userWalletId: UserWalletId) {
        onboardingRepository.storeVirtualAccountOrderFailed(userWalletId = userWalletId, isFailed = false)
        paymentAccountStatusFetcher.markVirtualAccountEligible(userWalletId)
    }
}