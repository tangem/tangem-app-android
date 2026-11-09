package com.tangem.domain.tangempay.usecase

import arrow.core.Either
import com.tangem.domain.models.account.BankCredentials
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.repository.OnboardingRepository
import com.tangem.domain.tangempay.error.TangemPayApiError

class GetBankCredentialsUseCase(
    private val onboardingRepository: OnboardingRepository,
) {
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        productInstanceId: String,
    ): Either<TangemPayApiError, BankCredentials> = onboardingRepository.getBankCredentials(
        userWalletId = userWalletId,
        productInstanceId = productInstanceId,
    )
}