package com.tangem.data.staking.multi

import arrow.core.Option
import arrow.core.some
import com.tangem.data.staking.store.P2PEthPoolBalancesStore
import com.tangem.data.staking.store.StakeKitBalancesStore
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
 * Combines staking balances from both StakeKit and P2PEthPool providers.
 *
 * @property params params
 * @property stakeKitBalancesStore StakeKit staking balances store
 * @property p2PEthPoolBalancesStore P2PEthPool balances store
 * @property dispatchers dispatchers
 *
[REDACTED_AUTHOR]
 */
internal class DefaultMultiStakingBalanceProducer @AssistedInject constructor(
    @Assisted val params: MultiStakingBalanceProducer.Params,
    private val stakeKitBalancesStore: StakeKitBalancesStore,
    private val p2PEthPoolBalancesStore: P2PEthPoolBalancesStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : MultiStakingBalanceProducer {

    override val fallback: Option<Set<StakingBalance>> = emptySet<StakingBalance>().some()

    override fun produce(): Flow<Set<StakingBalance>> {
        val stakeKitFlow = stakeKitBalancesStore.get(userWalletId = params.userWalletId)
        val p2pEthPoolFlow = p2PEthPoolBalancesStore.get(userWalletId = params.userWalletId)

        return combine(stakeKitFlow, p2pEthPoolFlow) { stakeKitBalances, p2pEthPoolBalances ->
            stakeKitBalances + p2pEthPoolBalances
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