package com.tangem.domain.pay.usecase

import arrow.core.Either
import com.tangem.domain.models.account.TangemPayCustomerTariffPlan
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.visa.error.VisaApiError

class GetCurrentTariffUseCase(
    private val onboardingRepository: OnboardingRepository,
) {
    suspend operator fun invoke(userWalletId: UserWalletId): Either<VisaApiError, TangemPayCustomerTariffPlan?> {
        return onboardingRepository.getCustomerInfo(userWalletId).map { it.tariffPlan }
    }
}