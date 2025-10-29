package com.tangem.domain.account.status.producer

import com.tangem.domain.core.flow.FlowProducer
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountStatus

/**
 * Producer that provides a single [AccountStatus] for a specific account identifier.
 *
[REDACTED_AUTHOR]
 */
interface SingleAccountStatusProducer : FlowProducer<AccountStatus> {

    data class Params(val accountId: AccountId)

    interface Factory : FlowProducer.Factory<Params, SingleAccountStatusProducer>
}