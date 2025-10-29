package com.tangem.domain.account.status.supplier

import com.tangem.domain.account.status.producer.SingleAccountStatusProducer
import com.tangem.domain.core.flow.FlowCachingSupplier
import com.tangem.domain.models.account.AccountStatus

/**
 * Supplier that provides a single [AccountStatus] for a specific account identifier.
 *
[REDACTED_AUTHOR]
 */
abstract class SingleAccountStatusSupplier(
    override val factory: SingleAccountStatusProducer.Factory,
    override val keyCreator: (SingleAccountStatusProducer.Params) -> String,
) : FlowCachingSupplier<SingleAccountStatusProducer, SingleAccountStatusProducer.Params, AccountStatus>()