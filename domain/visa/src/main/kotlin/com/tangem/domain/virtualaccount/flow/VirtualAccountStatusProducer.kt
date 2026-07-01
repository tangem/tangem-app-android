package com.tangem.domain.virtualaccount.flow

import com.tangem.domain.core.flow.FlowProducer
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.wallet.UserWalletId

interface VirtualAccountStatusProducer : FlowProducer<AccountStatus.Virtual> {
    data class Params(val userWalletId: UserWalletId)

    interface Factory : FlowProducer.Factory<Params, VirtualAccountStatusProducer>
}