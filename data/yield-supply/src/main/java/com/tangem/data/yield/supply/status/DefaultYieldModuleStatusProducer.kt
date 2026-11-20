package com.tangem.data.yield.supply.status

import arrow.core.Option
import arrow.core.some
import com.tangem.data.yield.supply.status.store.YieldModuleStatusStore
import com.tangem.domain.core.flow.FlowProducerTools
import com.tangem.domain.yield.supply.models.status.YieldModuleStatus
import com.tangem.domain.yield.supply.status.YieldModuleStatusProducer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn

internal class DefaultYieldModuleStatusProducer @AssistedInject constructor(
    @Assisted val params: YieldModuleStatusProducer.Params,
    override val flowProducerTools: FlowProducerTools,
    private val store: YieldModuleStatusStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : YieldModuleStatusProducer {

    override val fallback: Option<Set<YieldModuleStatus>> = emptySet<YieldModuleStatus>().some()

    override fun produce(): Flow<Set<YieldModuleStatus>> {
        return store.get(userWalletId = params.userWalletId)
            .distinctUntilChanged()
            .flowOn(dispatchers.default)
    }

    @AssistedFactory
    interface Factory : YieldModuleStatusProducer.Factory {
        override fun create(params: YieldModuleStatusProducer.Params): DefaultYieldModuleStatusProducer
    }
}