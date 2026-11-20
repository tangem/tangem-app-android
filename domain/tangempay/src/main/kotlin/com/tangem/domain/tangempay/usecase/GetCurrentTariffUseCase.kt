package com.tangem.domain.tangempay.usecase

import arrow.core.Either
import com.tangem.domain.models.account.TangemPayCustomerTariffPlan
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.repository.OnboardingRepository
import com.tangem.domain.tangempay.error.TangemPayApiError

class GetCurrentTariffUseCase(
    private val onboardingRepository: OnboardingRepository,
) {
    suspend operator fun invoke(userWalletId: UserWalletId): Either<TangemPayApiError, TangemPayCustomerTariffPlan?> {
        return onboardingRepository.getCustomerInfo(userWalletId).map { it.tariffPlan }
    }
}