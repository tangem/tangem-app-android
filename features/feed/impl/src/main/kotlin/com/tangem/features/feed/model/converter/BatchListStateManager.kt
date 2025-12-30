package com.tangem.features.feed.model.converter

import com.tangem.pagination.Batch
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

internal class BatchListStateManager<Key, Domain, UI>(
    private val converter: BatchItemConverter<Domain, UI>,
    private val dispatchers: CoroutineDispatcherProvider,
) {
    val state = MutableStateFlow(BatchListState<Key, Domain, UI>())

    suspend fun update(newList: List<Batch<Key, List<Domain>>>, forceUpdate: Boolean) =
        withContext(dispatchers.default) {
            state.update { currentState ->
                val uiBatches = currentState.uiBatches
                val previousList = currentState.processedItems

                if (newList.isEmpty()) {
                    return@update BatchListState(uiBatches = emptyList(), processedItems = emptyList())
                }

                val isInitialLoading = forceUpdate ||
                    previousList.isNullOrEmpty() ||
                    newList.firstOrNull()?.key != previousList.firstOrNull()?.key

                val outItems = if (isInitialLoading) {
                    newList.map { batch ->
                        Batch(key = batch.key, data = batch.data.map { converter.convert(it) })
                    }
                } else {
                    if (previousList.size != newList.size) {
                        val keysToAdd = newList.map { it.key }.subtract(previousList.map { it.key }.toSet())
                        val newBatches = newList.filter { keysToAdd.contains(it.key) }

                        uiBatches + newBatches.map { batch ->
                            Batch(key = batch.key, data = batch.data.map { converter.convert(it) })
                        }
                    } else {
                        uiBatches.mapIndexed { batchIndex, batch ->
                            val prevBatch = previousList[batchIndex]
                            val newBatch = newList[batchIndex]
                            if (prevBatch == newBatch) return@mapIndexed batch

                            Batch(
                                key = batch.key,
                                data = batch.data.mapIndexed { index, currentUiItem ->
                                    val prevItem = prevBatch.data.getOrNull(index)
                                    val newItem = newBatch.data.getOrNull(index)

                                    if (prevItem != null && newItem != null) {
                                        converter.update(prevItem, currentUiItem, newItem)
                                    } else if (newItem != null) {
                                        converter.convert(newItem)
                                    } else {
                                        currentUiItem
                                    }
                                },
                            )
                        }
                    }
                }

                coroutineContext.ensureActive()

                BatchListState(
                    uiBatches = outItems,
                    processedItems = newList,
                )
            }
        }
}

internal data class BatchListState<K, Domain, UI>(
    val uiBatches: List<Batch<K, List<UI>>> = emptyList(),
    val processedItems: List<Batch<K, List<Domain>>>? = emptyList(),
)

internal interface BatchItemConverter<Domain, UI> {
    fun convert(item: Domain): UI

    fun update(prevDomain: Domain, currentUI: UI, newDomain: Domain): UI {
        return convert(newDomain)
    }
}

internal fun <K, T> Flow<List<Batch<K, List<T>>>>.distinctBatchesContent(): Flow<List<Batch<K, List<T>>>> {
    return this.distinctUntilChanged { old, new ->
        old.size == new.size &&
            old.map { it.key } == new.map { it.key } &&
            old.flatMap { it.data } == new.flatMap { it.data }
    }
}