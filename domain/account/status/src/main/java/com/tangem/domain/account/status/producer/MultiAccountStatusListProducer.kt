package com.tangem.domain.account.status.producer

import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.core.flow.FlowProducer

/**
 * Produces a list of [AccountList]s for all user wallets.
 *
[REDACTED_AUTHOR]
 */
interface MultiAccountStatusListProducer : FlowProducer<List<AccountStatusList>> {

    interface Factory : FlowProducer.Factory<Unit, MultiAccountStatusListProducer>
}