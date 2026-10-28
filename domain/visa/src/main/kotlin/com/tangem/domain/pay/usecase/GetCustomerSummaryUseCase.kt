package com.tangem.domain.pay.usecase

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CustomerSummary
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.visa.error.VisaApiError

class GetCustomerSummaryUseCase(
    private val onboardingRepository: OnboardingRepository,
) {
    suspend operator fun invoke(userWalletId: UserWalletId): Either<VisaApiError, CustomerSummary> {
        return onboardingRepository.getCustomerSummary(userWalletId)
    }
}