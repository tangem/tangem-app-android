package com.tangem.data.polymarket.flow

import arrow.core.Option
import arrow.core.some
import com.tangem.data.polymarket.store.PredictionAccountStatusStore
import com.tangem.domain.core.flow.FlowProducerTools
import com.tangem.domain.models.account.PredictionAccountStatusValue
import com.tangem.domain.models.account.PredictionPositionsValue
import com.tangem.domain.models.getResultStatusSource
import com.tangem.domain.polymarket.flow.PredictionPositionsValueProducer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

internal class DefaultPredictionPositionsValueProducer @AssistedInject constructor(
    @Assisted private val params: PredictionPositionsValueProducer.Params,
    override val flowProducerTools: FlowProducerTools,
    private val statusStore: PredictionAccountStatusStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : PredictionPositionsValueProducer {

    override val fallback: Option<PredictionPositionsValue>
        get() = PredictionPositionsValue.Unknown.some()

    override fun produce(): Flow<PredictionPositionsValue> {
        return statusStore.get(userWalletId = params.userWalletId)
            .map { status -> (status as? PredictionAccountStatusValue.Active)?.positionsWithStatusSource() }
            .map { positions -> positions ?: PredictionPositionsValue.Unknown }
            .distinctUntilChanged()
            .flowOn(dispatchers.default)
    }

    private fun PredictionAccountStatusValue.Active.positionsWithStatusSource(): PredictionPositionsValue =
        when (val positions = positions) {
            is PredictionPositionsValue.Known -> positions.copy(
                source = listOf(source, positions.source).getResultStatusSource(),
            )
            PredictionPositionsValue.Unknown,
            PredictionPositionsValue.Unavailable,
            -> positions
        }

    @AssistedFactory
    interface Factory : PredictionPositionsValueProducer.Factory {
        override fun create(params: PredictionPositionsValueProducer.Params): DefaultPredictionPositionsValueProducer
    }
}