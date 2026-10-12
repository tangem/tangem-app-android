package com.tangem.domain.moneyaccount.flow

import arrow.core.Either
import com.tangem.domain.core.flow.FlowFetcher
import com.tangem.domain.models.wallet.UserWalletId

interface MoneyAccountStatusFetcher : FlowFetcher<MoneyAccountStatusFetcher.Params> {

    suspend operator fun invoke(userWalletId: UserWalletId): Either<Throwable, Unit> {
        return invoke(Params(userWalletId))
    }

    data class Params(val userWalletId: UserWalletId)
}