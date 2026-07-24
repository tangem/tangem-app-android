package com.tangem.domain.pay.usecase

import arrow.core.Either
import com.tangem.domain.models.account.BankCredentials
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.visa.error.VisaApiError

class GetBankCredentialsUseCase(
    private val onboardingRepository: OnboardingRepository,
) {
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        productInstanceId: String,
    ): Either<VisaApiError, BankCredentials> = onboardingRepository.getBankCredentials(
        userWalletId = userWalletId,
        productInstanceId = productInstanceId,
    )
}