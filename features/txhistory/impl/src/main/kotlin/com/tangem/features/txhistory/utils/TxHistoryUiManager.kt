package com.tangem.features.txhistory.utils

import com.tangem.core.ui.utils.toDateFormatWithTodayYesterday
import com.tangem.domain.txhistory.models.PaginationWrapper
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.features.txhistory.converter.TxHistoryItemToTransactionStateConverter
import com.tangem.features.txhistory.entity.TxHistoryUM
import com.tangem.pagination.Batch
import com.tangem.pagination.PaginationStatus
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapLatest
import java.util.UUID

internal class TxHistoryUiManager(
    private val state: MutableStateFlow<TxHistoryListState>,
    private val txHistoryItemConverter: TxHistoryItemToTransactionStateConverter,
    private val txHistoryUiActions: TxHistoryUiActions,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    val items: Flow<ImmutableList<TxHistoryUM.TxHistoryItemUM>> = state
        // filter initial states, since we dont emit loading items as UI items
        .filter { it.status !is PaginationStatus.None && it.status !is PaginationStatus.InitialLoading }
        .mapLatest { state ->
            state.uiBatches.asSequence()
                .flatMap { it.data }
                .toImmutableList()
        }
        .distinctUntilChanged()

    fun createOrUpdateUiBatches(
        newCurrencyBatches: List<Batch<Int, PaginationWrapper<TxHistoryItem>>>,
        clearUiBatches: Boolean,
    ): List<Batch<Int, List<TxHistoryUM.TxHistoryItemUM>>> {
        val currentUiBatches = state.value.uiBatches
        val batches = if (clearUiBatches) mutableListOf() else currentUiBatches.toMutableList()

        for ((key, data) in newCurrencyBatches) {
            // Find if batch with same key exists
            val existingBatchIndex = batches.indexOfFirst { it.key == key }
            val shouldUpdateExisting = existingBatchIndex != -1 &&
                currentUiBatches[existingBatchIndex].data.transactionItemsSizeNotEqual(data.items)

            // Case 1: Update existing batch if sizes differ
            if (shouldUpdateExisting) {
                val items = generateUiItems(key, data)
                batches[existingBatchIndex] = Batch(key = key, data = items)
                continue
            }

            // Case 2: Skip if batch exists and has same size
            if (existingBatchIndex != -1) {
                continue
            }

            // Case 3: Create new batch
            val items = generateUiItems(key, data)
            batches.add(Batch(key = key, data = items))
        }

        return batches
    }

    private fun generateUiItems(key: Int, data: PaginationWrapper<TxHistoryItem>): List<TxHistoryUM.TxHistoryItemUM> {
        val items = mutableListOf<TxHistoryUM.TxHistoryItemUM>()

        // Add title for the first batch
        if (key == 0) {
            items.add(TxHistoryUM.TxHistoryItemUM.Title(onExploreClick = txHistoryUiActions::openExplorer))
        }

        // Process batch items only if there are any
        if (data.items.isNotEmpty()) {
            // Add first item with its group title
            val firstItem = data.items.first()
            val firstDate = firstItem.timestampInMillis.toDateFormatWithTodayYesterday()

            items.add(
                TxHistoryUM.TxHistoryItemUM.GroupTitle(
                    title = firstDate,
                    itemKey = UUID.randomUUID().toString(),
                ),
            )
            items.add(TxHistoryUM.TxHistoryItemUM.Transaction(txHistoryItemConverter.convert(firstItem)))

            // Process remaining items with date separators when needed
            data.items.zipWithNext { current, next ->
                val currentDate = current.timestampInMillis.toDateFormatWithTodayYesterday()
                val nextDate = next.timestampInMillis.toDateFormatWithTodayYesterday()

                if (currentDate != nextDate) {
                    items.add(
                        TxHistoryUM.TxHistoryItemUM.GroupTitle(
                            title = nextDate,
                            itemKey = UUID.randomUUID().toString(),
                        ),
                    )
                }
                items.add(TxHistoryUM.TxHistoryItemUM.Transaction(txHistoryItemConverter.convert(next)))
            }
        }

        return items
    }

    private fun List<TxHistoryUM.TxHistoryItemUM>.transactionItemsSizeNotEqual(
        txHistoryItems: List<TxHistoryItem>,
    ): Boolean {
        return this.filterIsInstance<TxHistoryUM.TxHistoryItemUM.Transaction>().size != txHistoryItems.size
    }
}