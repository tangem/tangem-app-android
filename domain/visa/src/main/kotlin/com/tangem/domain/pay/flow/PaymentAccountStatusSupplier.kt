package com.tangem.domain.pay.flow

import com.tangem.domain.core.flow.FlowCachingSupplier
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

@Suppress("UnnecessaryAbstractClass")
abstract class PaymentAccountStatusSupplier(
    override val factory: PaymentAccountStatusProducer.Factory,
    override val keyCreator: (PaymentAccountStatusProducer.Params) -> String,
) : FlowCachingSupplier<PaymentAccountStatusProducer, PaymentAccountStatusProducer.Params, AccountStatus.Payment>() {

    operator fun invoke(userWalletId: UserWalletId): Flow<AccountStatus.Payment> {
        val params = PaymentAccountStatusProducer.Params(userWalletId)
        return this.invoke(params)
    }
}