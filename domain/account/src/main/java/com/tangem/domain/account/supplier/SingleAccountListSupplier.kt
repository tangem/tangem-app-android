package com.tangem.domain.account.supplier

import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.producer.SingleAccountListProducer
import com.tangem.domain.core.flow.FlowCachingSupplier

/**
 * Supplier that provides a single [AccountList] for a specific user wallet.
 *
[REDACTED_AUTHOR]
 */
abstract class SingleAccountListSupplier(
    override val factory: SingleAccountListProducer.Factory,
    override val keyCreator: (SingleAccountListProducer.Params) -> String,
) : FlowCachingSupplier<SingleAccountListProducer, SingleAccountListProducer.Params, AccountList>()