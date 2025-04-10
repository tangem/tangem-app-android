package com.tangem.domain.core.flow

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.retryWhen

/**
 * [Flow] producer
 *
 * @param Data data type of [Flow]
 *
[REDACTED_AUTHOR]
 */
interface FlowProducer<Data : Any> {

    /** Fallback value if [Flow] throws exception */
    val fallback: Data

    /** Produce [Flow] */
    fun produce(): Flow<Data>

    /** Produce [Flow] with retry mechanism */
    fun produceWithFallback(): Flow<Data> {
        return produce().retryWhen { _, _ ->
            emit(value = fallback)

            delay(timeMillis = 2000)

            true
        }
    }

    /** Factory for creating [Producer]. It helps to provide [Params] by constructor */
    interface Factory<Params : Any, Producer : FlowProducer<*>> {

        /** Create [Producer] using [params] */
        fun create(params: Params): Producer
    }
}