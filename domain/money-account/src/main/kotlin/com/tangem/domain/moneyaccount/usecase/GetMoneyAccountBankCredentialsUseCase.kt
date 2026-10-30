package com.tangem.domain.moneyaccount.usecase

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.tangem.domain.models.account.BankCredentials
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.visa.error.VisaApiError

class GetMoneyAccountBankCredentialsUseCase(
    private val onboardingRepository: OnboardingRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId): Either<VisaApiError, BankCredentials> = either {
        val savedInfo = onboardingRepository.getSavedCustomerInfo(userWalletId)
        val instance = savedInfo?.moneyAccountProductInstance
            ?: onboardingRepository.getCustomerInfo(userWalletId).bind().moneyAccountProductInstance
        val productInstance = ensureNotNull(instance) { VisaApiError.ProductInstanceNotFoundActivationRequired }

        onboardingRepository.getBankCredentials(
            userWalletId = userWalletId,
            productInstanceId = productInstance.id,
        ).bind()
    }
}