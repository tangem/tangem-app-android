package com.tangem.domain.moneyaccount.flow

import com.tangem.domain.core.flow.FlowCachingSupplier
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

open class MoneyAccountStatusSupplier(
    override val factory: MoneyAccountStatusProducer.Factory,
    override val keyCreator: (MoneyAccountStatusProducer.Params) -> String,
) : FlowCachingSupplier<MoneyAccountStatusProducer, MoneyAccountStatusProducer.Params, AccountStatus.Money>() {

    operator fun invoke(userWalletId: UserWalletId): Flow<AccountStatus.Money> {
        val params = MoneyAccountStatusProducer.Params(userWalletId)
        return this.invoke(params)
    }
}