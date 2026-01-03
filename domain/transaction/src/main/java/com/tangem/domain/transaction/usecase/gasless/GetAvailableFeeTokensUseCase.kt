package com.tangem.domain.transaction.usecase.gasless

import arrow.core.Either
import arrow.core.left
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.error.GetFeeError

class GetAvailableFeeTokensUseCase {

    operator fun invoke(userWalletId: UserWalletId, network: Network): Either<GetFeeError, List<CryptoCurrencyStatus>> {
        return GetFeeError.UnknownError.left()
    }
}