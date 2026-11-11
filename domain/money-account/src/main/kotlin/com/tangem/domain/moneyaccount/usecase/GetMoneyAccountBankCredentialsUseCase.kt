package com.tangem.domain.moneyaccount.usecase

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.tangem.domain.models.account.BankCredentials
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.repository.OnboardingRepository
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.utils.annotations.RemoveWithToggle

class GetMoneyAccountBankCredentialsUseCase(
    private val onboardingRepository: OnboardingRepository,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        @RemoveWithToggle(toggleName = "TWI_1778_MONEY_ACCOUNT_MXN_ENABLED") includeAllRails: Boolean,
    ): Either<TangemPayApiError, List<BankCredentials>> = either {
        val savedInfo = onboardingRepository.getSavedCustomerInfo(userWalletId)
        val instance = savedInfo?.moneyAccountProductInstance
            ?: onboardingRepository.getCustomerInfo(userWalletId).bind().moneyAccountProductInstance
        val productInstance = ensureNotNull(instance) { TangemPayApiError.ProductInstanceNotFoundActivationRequired }

        if (includeAllRails) {
            onboardingRepository.getAllBankCredentials(
                userWalletId = userWalletId,
                productInstanceId = productInstance.id,
            ).bind()
        } else {
            onboardingRepository.getBankCredentials(
                userWalletId = userWalletId,
                productInstanceId = productInstance.id,
            ).map { credentials -> listOf(credentials) }.bind()
        }
    }
}