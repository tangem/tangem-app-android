package com.tangem.domain.account.supplier

import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.producer.MultiAccountListProducer
import com.tangem.domain.core.flow.FlowCachingSupplier
import kotlinx.coroutines.flow.Flow

/**
 * Supplier that provides a list of [AccountList]s for all user wallets.
 *
[REDACTED_AUTHOR]
 */
abstract class MultiAccountListSupplier(
    override val factory: MultiAccountListProducer.Factory,
    override val keyCreator: (Unit) -> String,
) : FlowCachingSupplier<MultiAccountListProducer, Unit, List<AccountList>>() {

    operator fun invoke(): Flow<List<AccountList>> {
        return super.invoke(params = Unit)
    }
}