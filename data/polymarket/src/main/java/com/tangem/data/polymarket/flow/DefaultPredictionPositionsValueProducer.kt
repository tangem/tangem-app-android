package com.tangem.data.polymarket.flow

import arrow.core.Option
import arrow.core.some
import com.tangem.data.polymarket.store.PredictionPositionsValueStore
import com.tangem.domain.core.flow.FlowProducerTools
import com.tangem.domain.polymarket.flow.PredictionPositionsValueProducer
import com.tangem.domain.polymarket.model.PredictionPositionsValue
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

internal class DefaultPredictionPositionsValueProducer @AssistedInject constructor(
    @Assisted private val params: PredictionPositionsValueProducer.Params,
    override val flowProducerTools: FlowProducerTools,
    private val store: PredictionPositionsValueStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : PredictionPositionsValueProducer {

    override val fallback: Option<PredictionPositionsValue>
        get() = PredictionPositionsValue.Unknown.some()

    override fun produce(): Flow<PredictionPositionsValue> {
        return flow {
            emit(store.getSyncOrNull(userWalletId = params.userWalletId) ?: PredictionPositionsValue.Unknown)
            emitAll(store.get(userWalletId = params.userWalletId))
        }
            .distinctUntilChanged()
            .flowOn(dispatchers.default)
    }

    @AssistedFactory
    interface Factory : PredictionPositionsValueProducer.Factory {
        override fun create(params: PredictionPositionsValueProducer.Params): DefaultPredictionPositionsValueProducer
    }
}