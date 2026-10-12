package com.tangem.domain.pay.usecase

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.AccountFee
import com.tangem.domain.pay.model.AccountFeeGroupType
import com.tangem.domain.visa.error.VisaApiError

class GetOnrampFeesUseCase(
    private val getAccountFeesUseCase: GetAccountFeesUseCase,
) {
    suspend operator fun invoke(userWalletId: UserWalletId): Either<VisaApiError, List<AccountFee>> {
        return getAccountFeesUseCase(
            userWalletId = userWalletId,
            types = setOf(AccountFeeGroupType.ONRAMP),
        )
    }
}