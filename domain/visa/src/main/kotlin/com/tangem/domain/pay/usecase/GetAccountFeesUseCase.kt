package com.tangem.domain.pay.usecase

import arrow.core.Either
import com.tangem.domain.models.pay.TangemPayProduct
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.AccountFee
import com.tangem.domain.pay.model.AccountFeeGroupType
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.visa.error.VisaApiError

class GetAccountFeesUseCase(
    private val onboardingRepository: OnboardingRepository,
) {
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        product: TangemPayProduct,
        types: Set<AccountFeeGroupType>,
    ): Either<VisaApiError, List<AccountFee>> {
        return onboardingRepository.getFees(userWalletId = userWalletId, product = product, types = types)
    }
}