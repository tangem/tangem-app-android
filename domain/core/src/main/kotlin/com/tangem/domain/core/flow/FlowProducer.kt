package com.tangem.domain.core.flow

import arrow.core.Option
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.retryWhen

/**
 * [Flow] producer
 *
 * @param Data data type of [Flow]
 *
[REDACTED_AUTHOR]
 */
interface FlowProducer<Data> {

    /** Fallback value if [Flow] throws exception */
    val fallback: Option<Data>

    /** Produce [Flow] */
    fun produce(): Flow<Data>

    /** Produce [Flow] with retry mechanism */
    fun produceWithFallback(): Flow<Data> {
        return produce().retryWhen { _, _ ->
            retryWhen(this)
        }
    }

    suspend fun retryWhen(collector: FlowCollector<Data>): Boolean {
        fallback.onSome { collector.emit(value = it) }

        delay(timeMillis = 2000)

        return true
    }

    /** Factory for creating [Producer]. It helps to provide [Params] by constructor */
    interface Factory<Params : Any, Producer : FlowProducer<*>> {

        /** Create [Producer] using [params] */
        fun create(params: Params): Producer
    }
}

interface FlowProducerScope : CoroutineScope

interface FlowProducerTools {

    fun <T> shareInProducer(flow: Flow<T>, flowProducer: FlowProducer<T>, withRetryWhen: Boolean = true): SharedFlow<T>

    companion object {

        fun <T> Flow<T>.shareInProducer(
            producerTools: FlowProducerTools,
            flowProducer: FlowProducer<T>,
            withRetryWhen: Boolean = true,
        ): SharedFlow<T> = producerTools
            .shareInProducer(this, flowProducer, withRetryWhen)
    }
}