package com.tangem.domain.yield.supply.status

import com.tangem.domain.core.flow.FlowProducer
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.models.status.YieldModuleStatus

/** Producer of all Yield Module statuses of the wallet [Params.userWalletId] across its networks */
interface YieldModuleStatusProducer : FlowProducer<Set<YieldModuleStatus>> {

    data class Params(val userWalletId: UserWalletId)

    interface Factory : FlowProducer.Factory<Params, YieldModuleStatusProducer>
}