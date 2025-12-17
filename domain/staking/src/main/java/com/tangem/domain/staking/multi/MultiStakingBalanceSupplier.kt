package com.tangem.domain.staking.multi

import com.tangem.domain.core.flow.FlowCachingSupplier
import com.tangem.domain.core.flow.FlowProducer
import com.tangem.domain.models.staking.StakingBalance

/**
 * Supplier of all staking balances for selected wallet [MultiStakingBalanceProducer.Params]
 *
 * @property factory    factory for creating [MultiStakingBalanceProducer]
 * @property keyCreator key creator
 *
[REDACTED_AUTHOR]
 */
abstract class MultiStakingBalanceSupplier(
    override val factory: FlowProducer.Factory<MultiStakingBalanceProducer.Params, MultiStakingBalanceProducer>,
    override val keyCreator: (MultiStakingBalanceProducer.Params) -> String,
) : FlowCachingSupplier<MultiStakingBalanceProducer, MultiStakingBalanceProducer.Params, Set<StakingBalance>>()