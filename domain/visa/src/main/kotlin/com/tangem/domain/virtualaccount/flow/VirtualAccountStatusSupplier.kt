package com.tangem.domain.virtualaccount.flow

import com.tangem.domain.core.flow.FlowCachingSupplier
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

open class VirtualAccountStatusSupplier(
    override val factory: VirtualAccountStatusProducer.Factory,
    override val keyCreator: (VirtualAccountStatusProducer.Params) -> String,
) : FlowCachingSupplier<VirtualAccountStatusProducer, VirtualAccountStatusProducer.Params, AccountStatus.Virtual>() {

    operator fun invoke(userWalletId: UserWalletId): Flow<AccountStatus.Virtual> {
        val params = VirtualAccountStatusProducer.Params(userWalletId)
        return this.invoke(params)
    }
}