package com.tangem.data.staking.multi

import arrow.core.Option
import arrow.core.some
import com.tangem.data.staking.store.P2PBalancesStore
import com.tangem.data.staking.store.YieldsBalancesStore
import com.tangem.domain.models.staking.YieldBalance
import com.tangem.domain.staking.multi.MultiYieldBalanceProducer
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
 * Default implementation of [MultiYieldBalanceProducer]
 *
 * Combines yield balances from both StakeKit and P2P providers.
 *
 * @property params params
 * @property yieldsBalancesStore StakeKit yields balances store
 * @property p2pBalancesStore P2P balances store
 * @property dispatchers dispatchers
 *
[REDACTED_AUTHOR]
 */
internal class DefaultMultiYieldBalanceProducer @AssistedInject constructor(
    @Assisted val params: MultiYieldBalanceProducer.Params,
    private val yieldsBalancesStore: YieldsBalancesStore,
    private val p2pBalancesStore: P2PBalancesStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : MultiYieldBalanceProducer {

    override val fallback: Option<Set<YieldBalance>> = emptySet<YieldBalance>().some()

    override fun produce(): Flow<Set<YieldBalance>> {
        val stakeKitFlow = yieldsBalancesStore.get(userWalletId = params.userWalletId)
        val p2pFlow = p2pBalancesStore.get(userWalletId = params.userWalletId)

        return combine(stakeKitFlow, p2pFlow) { stakeKitBalances, p2pBalances ->
            stakeKitBalances + p2pBalances
        }
            .distinctUntilChanged()
            .onEmpty { emit(value = hashSetOf()) }
            .flowOn(dispatchers.default)
    }

    @AssistedFactory
    interface Factory : MultiYieldBalanceProducer.Factory {
        override fun create(params: MultiYieldBalanceProducer.Params): DefaultMultiYieldBalanceProducer
    }
}