package com.tangem.domain.account.producer

import com.tangem.domain.account.models.AccountList
import com.tangem.domain.core.flow.FlowProducer
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Produces a list of [AccountList] for a specific user wallet.
 *
[REDACTED_AUTHOR]
 */
interface SingleAccountListProducer : FlowProducer<AccountList> {

    data class Params(val userWalletId: UserWalletId)

    interface Factory : FlowProducer.Factory<Params, SingleAccountListProducer>
}