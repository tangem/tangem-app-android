package com.tangem.data.staking.multi

import arrow.core.Option
import arrow.core.some
import com.tangem.data.staking.store.P2PBalancesStore
import com.tangem.data.staking.store.StakingBalancesStore
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.staking.multi.MultiStakingBalanceProducer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEmpty

/**
 * Default implementation of [MultiStakingBalanceProducer]
 *
 * Combines staking balances from both StakeKit and P2P providers.
 *
 * @property params params
 * @property stakingBalancesStore StakeKit staking balances store
 * @property p2pBalancesStore P2P balances store
 * @property dispatchers dispatchers
 *
[REDACTED_AUTHOR]
 */
internal class DefaultMultiStakingBalanceProducer @AssistedInject constructor(
    @Assisted val params: MultiStakingBalanceProducer.Params,
    private val stakingBalancesStore: StakingBalancesStore,
    private val p2pBalancesStore: P2PBalancesStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : MultiStakingBalanceProducer {

    override val fallback: Option<Set<StakingBalance>> = emptySet<StakingBalance>().some()

    override fun produce(): Flow<Set<StakingBalance>> {
        val stakeKitFlow = stakingBalancesStore.get(userWalletId = params.userWalletId)
        val p2pFlow = p2pBalancesStore.get(userWalletId = params.userWalletId)

        return combine(stakeKitFlow, p2pFlow) { stakeKitBalances, p2pBalances ->
            stakeKitBalances + p2pBalances
        }
            .distinctUntilChanged()
            .onEmpty { emit(value = hashSetOf()) }
            .flowOn(dispatchers.default)
    }

    @AssistedFactory
    interface Factory : MultiStakingBalanceProducer.Factory {
        override fun create(params: MultiStakingBalanceProducer.Params): DefaultMultiStakingBalanceProducer
    }
}