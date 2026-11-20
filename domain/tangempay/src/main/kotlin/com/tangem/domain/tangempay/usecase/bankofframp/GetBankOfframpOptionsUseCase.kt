package com.tangem.domain.tangempay.usecase.bankofframp

import arrow.core.Either
import com.tangem.domain.models.tangempay.TangemPayProduct
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.bankofframp.BankOfframpError
import com.tangem.domain.tangempay.model.bankofframp.BankOfframpOptions
import com.tangem.domain.tangempay.repository.BankOfframpRepository

class GetBankOfframpOptionsUseCase(
    private val repository: BankOfframpRepository,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        product: TangemPayProduct,
    ): Either<BankOfframpError, BankOfframpOptions> {
        return repository.getOptions(userWalletId = userWalletId, product = product)
    }
}