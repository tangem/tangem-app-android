package com.tangem.domain.account.supplier

import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.producer.SingleAccountListProducer
import com.tangem.domain.core.flow.FlowCachingSupplier
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

/**
 * Supplier that provides a single [AccountList] for a specific user wallet.
 *
[REDACTED_AUTHOR]
 */
abstract class SingleAccountListSupplier(
    override val factory: SingleAccountListProducer.Factory,
    override val keyCreator: (SingleAccountListProducer.Params) -> String,
) : FlowCachingSupplier<SingleAccountListProducer, SingleAccountListProducer.Params, AccountList>() {

    operator fun invoke(userWalletId: UserWalletId): Flow<AccountList> {
        return super.invoke(
            params = SingleAccountListProducer.Params(userWalletId = userWalletId),
        )
    }
}