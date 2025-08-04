package com.tangem.data.staking.multi

import com.tangem.data.staking.store.YieldsBalancesStore
import com.tangem.domain.models.staking.YieldBalance
import com.tangem.domain.staking.multi.MultiYieldBalanceProducer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEmpty

/**
 * Default implementation of [MultiYieldBalanceProducer]
 *
 * @property params params
 * @property yieldsBalancesStore yields balances store
 * @property dispatchers dispatchers
 *
[REDACTED_AUTHOR]
 */
internal class DefaultMultiYieldBalanceProducer @AssistedInject constructor(
    @Assisted val params: MultiYieldBalanceProducer.Params,
    private val yieldsBalancesStore: YieldsBalancesStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : MultiYieldBalanceProducer {

    override val fallback: Set<YieldBalance>
        get() = setOf()

    override fun produce(): Flow<Set<YieldBalance>> {
        return yieldsBalancesStore.get(userWalletId = params.userWalletId)
            .distinctUntilChanged()
            .onEmpty { emit(value = hashSetOf()) }
            .flowOn(dispatchers.default)
    }

    @AssistedFactory
    interface Factory : MultiYieldBalanceProducer.Factory {
        override fun create(params: MultiYieldBalanceProducer.Params): DefaultMultiYieldBalanceProducer
    }
}