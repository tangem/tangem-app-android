package com.tangem.domain.pay.usecase

import arrow.core.Either
import com.tangem.domain.models.account.TangemPayOnrampFee
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.visa.error.VisaApiError

class GetOnrampFeesUseCase(
    private val onboardingRepository: OnboardingRepository,
) {
    suspend operator fun invoke(userWalletId: UserWalletId): Either<VisaApiError, List<TangemPayOnrampFee>> =
        onboardingRepository.getOnrampFees(userWalletId)
}