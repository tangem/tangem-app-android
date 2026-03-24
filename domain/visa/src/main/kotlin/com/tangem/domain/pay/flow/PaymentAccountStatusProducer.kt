package com.tangem.domain.pay.flow

import com.tangem.domain.core.flow.FlowProducer
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.wallet.UserWalletId

interface PaymentAccountStatusProducer : FlowProducer<AccountStatus.Payment> {
    data class Params(val userWalletId: UserWalletId)

    interface Factory : FlowProducer.Factory<Params, PaymentAccountStatusProducer>
}