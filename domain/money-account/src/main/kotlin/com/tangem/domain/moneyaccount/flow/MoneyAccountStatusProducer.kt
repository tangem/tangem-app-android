package com.tangem.domain.moneyaccount.flow

import com.tangem.domain.core.flow.FlowProducer
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.wallet.UserWalletId

interface MoneyAccountStatusProducer : FlowProducer<AccountStatus.Money> {
    data class Params(val userWalletId: UserWalletId)

    interface Factory : FlowProducer.Factory<Params, MoneyAccountStatusProducer>
}