package com.tangem.domain.tangempay.usecase

import arrow.core.Either
import com.tangem.domain.models.tangempay.TangemPayProduct
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.AccountFee
import com.tangem.domain.tangempay.model.AccountFeeGroupType
import com.tangem.domain.tangempay.error.TangemPayApiError

class GetOnrampFeesUseCase(
    private val getAccountFeesUseCase: GetAccountFeesUseCase,
) {
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        product: TangemPayProduct,
    ): Either<TangemPayApiError, List<AccountFee>> {
        return getAccountFeesUseCase(
            userWalletId = userWalletId,
            product = product,
            types = setOf(AccountFeeGroupType.ONRAMP),
        )
    }
}