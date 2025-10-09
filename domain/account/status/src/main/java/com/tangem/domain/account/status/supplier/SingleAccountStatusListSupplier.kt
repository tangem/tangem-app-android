package com.tangem.domain.account.status.supplier

import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.producer.SingleAccountStatusListProducer
import com.tangem.domain.core.flow.FlowCachingSupplier
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

/**
 * Supplier that provides a single [AccountStatusList] for a specific user wallet.
 *
[REDACTED_AUTHOR]
 */
abstract class SingleAccountStatusListSupplier(
    override val factory: SingleAccountStatusListProducer.Factory,
    override val keyCreator: (SingleAccountStatusListProducer.Params) -> String,
) : FlowCachingSupplier<SingleAccountStatusListProducer, SingleAccountStatusListProducer.Params, AccountStatusList>() {

    operator fun invoke(userWalletId: UserWalletId): Flow<AccountStatusList> {
        val params = SingleAccountStatusListProducer.Params(userWalletId)
        return this.invoke(params)
    }
}