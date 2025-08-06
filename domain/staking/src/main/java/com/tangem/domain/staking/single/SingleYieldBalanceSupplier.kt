package com.tangem.domain.staking.single

import com.tangem.domain.core.flow.FlowCachingSupplier
import com.tangem.domain.core.flow.FlowProducer
import com.tangem.domain.models.staking.YieldBalance

/**
 * Supplier of yield balance for selected wallet [SingleYieldBalanceProducer.Params]
 *
 * @property factory    factory for creating [SingleYieldBalanceProducer]
 * @property keyCreator key creator
 *
[REDACTED_AUTHOR]
 */
abstract class SingleYieldBalanceSupplier(
    override val factory: FlowProducer.Factory<SingleYieldBalanceProducer.Params, SingleYieldBalanceProducer>,
    override val keyCreator: (SingleYieldBalanceProducer.Params) -> String,
) : FlowCachingSupplier<SingleYieldBalanceProducer, SingleYieldBalanceProducer.Params, YieldBalance>()