package com.tangem.domain.yield.supply.status

import com.tangem.domain.core.flow.FlowCachingSupplier
import com.tangem.domain.core.flow.FlowProducer
import com.tangem.domain.yield.supply.models.status.YieldModuleStatus

/** Supplier of all Yield Module statuses of the wallet [YieldModuleStatusProducer.Params.userWalletId] */
open class YieldModuleStatusSupplier(
    override val factory: FlowProducer.Factory<YieldModuleStatusProducer.Params, YieldModuleStatusProducer>,
    override val keyCreator: (YieldModuleStatusProducer.Params) -> String,
) : FlowCachingSupplier<YieldModuleStatusProducer, YieldModuleStatusProducer.Params, Set<YieldModuleStatus>>()