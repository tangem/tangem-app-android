package com.tangem.domain.account.status.supplier

import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.producer.SingleAccountStatusListProducer
import com.tangem.domain.core.flow.FlowCachingSupplier

/**
 * Supplier that provides a single [AccountStatusList] for a specific user wallet.
 *
[REDACTED_AUTHOR]
 */
abstract class SingleAccountStatusListSupplier(
    override val factory: SingleAccountStatusListProducer.Factory,
    override val keyCreator: (SingleAccountStatusListProducer.Params) -> String,
) : FlowCachingSupplier<SingleAccountStatusListProducer, SingleAccountStatusListProducer.Params, AccountStatusList>()