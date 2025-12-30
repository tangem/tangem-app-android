package com.tangem.domain.staking.single

import com.tangem.domain.core.flow.FlowCachingSupplier
import com.tangem.domain.core.flow.FlowProducer
import com.tangem.domain.models.staking.StakingBalance

/**
 * Supplier of staking balance for selected wallet [SingleStakingBalanceProducer.Params]
 *
 * @property factory    factory for creating [SingleStakingBalanceProducer]
 * @property keyCreator key creator
 *
[REDACTED_AUTHOR]
 */
abstract class SingleStakingBalanceSupplier(
    override val factory: FlowProducer.Factory<SingleStakingBalanceProducer.Params, SingleStakingBalanceProducer>,
    override val keyCreator: (SingleStakingBalanceProducer.Params) -> String,
) : FlowCachingSupplier<SingleStakingBalanceProducer, SingleStakingBalanceProducer.Params, StakingBalance>()