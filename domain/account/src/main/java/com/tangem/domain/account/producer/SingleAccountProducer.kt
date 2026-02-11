package com.tangem.domain.account.producer

import com.tangem.domain.core.flow.FlowProducer
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId

/**
 * Produces a flow of [Account] for a single account identified by [Params.accountId].
 * The flow emits updates whenever the account's portfolio changes.
 */
interface SingleAccountProducer : FlowProducer<Account> {

    data class Params(val accountId: AccountId)

    interface Factory : FlowProducer.Factory<Params, SingleAccountProducer>
}