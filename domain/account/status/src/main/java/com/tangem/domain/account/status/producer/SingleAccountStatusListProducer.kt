package com.tangem.domain.account.status.producer

import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.core.flow.FlowProducer
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Produces a [AccountStatusList] for a specific user wallet.
 *
[REDACTED_AUTHOR]
 */
interface SingleAccountStatusListProducer : FlowProducer<AccountStatusList> {

    data class Params(val userWalletId: UserWalletId)

    interface Factory : FlowProducer.Factory<Params, SingleAccountStatusListProducer>
}