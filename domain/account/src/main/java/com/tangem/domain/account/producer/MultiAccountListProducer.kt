package com.tangem.domain.account.producer

import com.tangem.domain.account.models.AccountList
import com.tangem.domain.core.flow.FlowProducer

/**
 * Produces a list of [AccountList]s for all user wallets.
 *
[REDACTED_AUTHOR]
 */
interface MultiAccountListProducer : FlowProducer<List<AccountList>> {

    interface Factory : FlowProducer.Factory<Unit, MultiAccountListProducer>
}