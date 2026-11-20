package com.tangem.domain.tangempay.usecase

import arrow.core.Either
import com.tangem.domain.models.tangempay.TangemPayEligibility
import com.tangem.domain.models.tangempay.TangemPayProduct
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.repository.TangemPayResidencyRepository
import com.tangem.domain.tangempay.error.TangemPayApiError

class CheckTangemPayEligibilityForCustomerUseCase(
    private val residencyRepository: TangemPayResidencyRepository,
) {
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        product: TangemPayProduct,
    ): Either<TangemPayApiError, TangemPayEligibility> = residencyRepository.checkEligibilityForCustomer(
        userWalletId = userWalletId,
        product = product,
    )
}