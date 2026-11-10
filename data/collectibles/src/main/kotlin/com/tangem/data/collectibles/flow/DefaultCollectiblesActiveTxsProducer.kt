package com.tangem.data.collectibles.flow

import arrow.core.Option
import arrow.core.none
import com.tangem.data.collectibles.store.CollectiblesActiveTxsStore
import com.tangem.domain.collectibles.flow.CollectiblesActiveTxsProducer
import com.tangem.domain.collectibles.models.CollectibleActiveTx
import com.tangem.domain.core.flow.FlowProducerTools
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow

internal class DefaultCollectiblesActiveTxsProducer @AssistedInject constructor(
    @Assisted private val params: CollectiblesActiveTxsProducer.Params,
    override val flowProducerTools: FlowProducerTools,
    private val activeTxsStore: CollectiblesActiveTxsStore,
) : CollectiblesActiveTxsProducer {

    override val fallback: Option<List<CollectibleActiveTx>> = none()

    override fun produce(): Flow<List<CollectibleActiveTx>> = activeTxsStore.get(params.userWalletId)

    @AssistedFactory
    interface Factory : CollectiblesActiveTxsProducer.Factory {
        override fun create(params: CollectiblesActiveTxsProducer.Params): DefaultCollectiblesActiveTxsProducer
    }
}