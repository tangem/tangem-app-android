package com.tangem.domain.jointaccount.fetcher

import arrow.core.Either
import com.tangem.domain.core.flow.FlowFetcher
import com.tangem.domain.models.wallet.UserWalletId

interface SingleJointAccountListFetcher : FlowFetcher<SingleJointAccountListFetcher.Params> {

    suspend operator fun invoke(userWalletId: UserWalletId): Either<Throwable, Unit> {
        return invoke(params = Params(userWalletId))
    }

    data class Params(val userWalletId: UserWalletId)
}