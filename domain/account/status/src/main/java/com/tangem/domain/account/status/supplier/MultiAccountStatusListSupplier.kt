package com.tangem.domain.account.status.supplier

import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.producer.MultiAccountStatusListProducer
import com.tangem.domain.core.flow.FlowCachingSupplier

/**
 * Supplier that provides a list of [AccountStatusList]s for all user wallets.
 *
[REDACTED_AUTHOR]
 */
abstract class MultiAccountStatusListSupplier(
    override val factory: MultiAccountStatusListProducer.Factory,
    override val keyCreator: (Unit) -> String,
) : FlowCachingSupplier<MultiAccountStatusListProducer, Unit, List<AccountStatusList>>()