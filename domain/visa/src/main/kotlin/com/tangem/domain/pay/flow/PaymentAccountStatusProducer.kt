package com.tangem.domain.pay.flow

import com.tangem.domain.core.flow.FlowProducer
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.PaymentAccountStatus

interface PaymentAccountStatusProducer : FlowProducer<PaymentAccountStatus> {
    data class Params(val userWalletId: UserWalletId)

    interface Factory : FlowProducer.Factory<Params, PaymentAccountStatusProducer>
}