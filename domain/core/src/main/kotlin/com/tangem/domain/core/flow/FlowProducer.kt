package com.tangem.domain.core.flow

import arrow.core.Option
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

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
    val flowProducerTools: FlowProducerTools

    /** Produce [Flow] */
    fun produce(): Flow<Data>

    /** Produce [Flow] with retry mechanism */
    fun produceWithFallback(): Flow<Data> {
        return flowProducerTools.shareInProducer(
            flow = produce(),
            flowProducer = this,
        )
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
}