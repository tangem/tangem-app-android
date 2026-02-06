package com.tangem.domain.tangempay

import arrow.core.Either
import com.tangem.core.error.UniversalError
import com.tangem.domain.models.wallet.UserWalletId

interface GetTangemPayCustomerIdUseCase {

    operator fun invoke(userWalletId: UserWalletId): Either<UniversalError, String>
}