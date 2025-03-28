package com.tangem.domain.core.flow

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update

private typealias FlowsStore<T> = MutableStateFlow<Map<String, Flow<T>>>

/**
 * [Flow] supplier with caching mechanism
 *
 * @param Producer      producer of flow [Flow]
 * @param Params        type of data that required to get [Flow]
 * @param Data          data type of [Flow]
 * @property flowsStore store of flows
 */
abstract class FlowCachingSupplier<Producer : FlowProducer<Data>, Params : Any, Data : Any>(
    private val flowsStore: FlowsStore<Data> = MutableStateFlow(value = emptyMap()),
) : FlowSupplier<Params, Data> {

    /** Factory of [FlowProducer] */
    abstract val factory: FlowProducer.Factory<Params, Producer>

    /** Key creator */
    abstract val keyCreator: (Params) -> String

    /**
     * Supply [Flow] by [params].

     */
    override operator fun invoke(params: Params): Flow<Data> {
        val key = keyCreator(params)
        val saved = flowsStore.value[key]

        if (saved != null) return saved

        val flowProducer = factory.create(params = params)

        return runCatching(flowProducer::produceWithFallback)
            .onSuccess { flow ->
                flowsStore.update {
                    it.toMutableMap().apply {
                        put(key = key, value = flow)
                    }
                }
            }
            .getOrThrow()
            .catch { cause ->
                flowsStore.update {
                    it.toMutableMap().apply {
                        remove(key)
                    }
                }

                throw cause
            }
    }
}