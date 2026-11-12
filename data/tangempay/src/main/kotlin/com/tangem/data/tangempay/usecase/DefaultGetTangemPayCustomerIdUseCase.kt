package com.tangem.data.tangempay.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.core.error.UniversalError
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.repository.OnboardingRepository
import com.tangem.domain.tangempay.GetTangemPayCustomerIdUseCase
import com.tangem.domain.tangempay.error.TangemPayApiError
import javax.inject.Inject

internal class DefaultGetTangemPayCustomerIdUseCase @Inject constructor(
    private val tangemPayOnboardingRepository: OnboardingRepository,
) : GetTangemPayCustomerIdUseCase {

    override fun invoke(userWalletId: UserWalletId): Either<UniversalError, String> {
        val customerId = tangemPayOnboardingRepository.getSavedCustomerInfo(userWalletId)?.customerId
        return if (customerId.isNullOrEmpty()) {
            TangemPayApiError.CustomerIdUnavailable.left()
        } else {
            customerId.right()
        }
    }
}