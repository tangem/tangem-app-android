package com.tangem.domain.pay.flow

import arrow.core.Either
import com.tangem.domain.core.flow.FlowFetcher
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.account.VirtualAccountOnramp
import com.tangem.domain.models.wallet.UserWalletId

interface PaymentAccountStatusFetcher : FlowFetcher<PaymentAccountStatusFetcher.Params> {

    suspend operator fun invoke(userWalletId: UserWalletId): Either<Throwable, Unit> {
        return invoke(Params(userWalletId))
    }

    /**
     * Optimistically marks the cached VA on-ramp as [VirtualAccountOnramp.Processing] so the UI reflects a

     * cached [PaymentAccountStatusValue.Loaded].
     */
    suspend fun markVirtualAccountProcessing(userWalletId: UserWalletId)

    data class Params(val userWalletId: UserWalletId)
}