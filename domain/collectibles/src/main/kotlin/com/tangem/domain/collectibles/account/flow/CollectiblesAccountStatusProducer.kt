package com.tangem.domain.collectibles.account.flow

import com.tangem.domain.core.flow.FlowProducer
import com.tangem.domain.models.account.CollectiblesAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Producer of the collectibles account status *value* of a single wallet — wrapping it into an account status is
 * the job of whoever assembles the wallet's account list.
 */
interface CollectiblesAccountStatusProducer : FlowProducer<CollectiblesAccountStatusValue> {

    data class Params(val userWalletId: UserWalletId)

    interface Factory : FlowProducer.Factory<Params, CollectiblesAccountStatusProducer>
}