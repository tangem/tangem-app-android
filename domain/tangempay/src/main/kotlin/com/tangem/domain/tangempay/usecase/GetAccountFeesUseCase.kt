package com.tangem.domain.tangempay.usecase

import arrow.core.Either
import com.tangem.domain.models.tangempay.TangemPayProduct
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.AccountFee
import com.tangem.domain.tangempay.model.AccountFeeGroupType
import com.tangem.domain.tangempay.repository.OnboardingRepository
import com.tangem.domain.tangempay.error.TangemPayApiError

class GetAccountFeesUseCase(
    private val onboardingRepository: OnboardingRepository,
) {
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        product: TangemPayProduct,
        types: Set<AccountFeeGroupType>,
    ): Either<TangemPayApiError, List<AccountFee>> {
        return onboardingRepository.getFees(userWalletId = userWalletId, product = product, types = types)
    }
}