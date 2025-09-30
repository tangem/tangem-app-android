package com.tangem.features.tangempay.utils

import com.tangem.core.ui.utils.toDateFormatWithTodayYesterday
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.features.tangempay.model.transformers.TangemPayTxHistoryItemsConverter
import com.tangem.features.txhistory.entity.TxHistoryUM
import com.tangem.features.txhistory.utils.TxHistoryUiActions
import com.tangem.pagination.Batch
import com.tangem.pagination.PaginationStatus
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.util.UUID

internal class TangemPayTxHistoryUiManager(
    private val state: MutableStateFlow<TangemPayTxHistoryState>,
    private val txHistoryUiActions: TxHistoryUiActions,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    val items: Flow<ImmutableList<TxHistoryUM.TxHistoryItemUM>> = state
        // filter initial states, since we dont emit loading items as UI items
        .filter {
            it.status !is PaginationStatus.None &&
                it.status !is PaginationStatus.InitialLoading &&
                it.status !is PaginationStatus.InitialLoadingError
        }
        .mapLatest { state ->
            state.uiBatches.asSequence()
                .flatMap { it.data }
                .toImmutableList()
        }
        .distinctUntilChanged()

    private val txHistoryItemConverter = TangemPayTxHistoryItemsConverter(txHistoryUiActions = txHistoryUiActions)

    fun createOrUpdateUiBatches(
        newCurrencyBatches: List<Batch<Int, List<TangemPayTxHistoryItem>>>,
        clearUiBatches: Boolean,
    ): List<Batch<Int, List<TxHistoryUM.TxHistoryItemUM>>> {
        val currentUiBatches = state.value.uiBatches
        val batches = if (clearUiBatches) mutableListOf() else currentUiBatches.toMutableList()

        for ((key, data) in newCurrencyBatches) {
            // Find if batch with same key exists
            val existingBatchIndex = batches.indexOfFirst { it.key == key }
            val shouldUpdateExisting = existingBatchIndex != -1 &&
                currentUiBatches[existingBatchIndex].data.transactionItemsSizeNotEqual(data)

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

    private fun generateUiItems(key: Int, data: List<TangemPayTxHistoryItem>): List<TxHistoryUM.TxHistoryItemUM> {
        val items = mutableListOf<TxHistoryUM.TxHistoryItemUM>()

        // Add title for the first batch
        if (key == 0) {
            items.add(TxHistoryUM.TxHistoryItemUM.Title(onExploreClick = txHistoryUiActions::openExplorer))
        }

        // Process batch items only if there are any
        if (data.isNotEmpty()) {
            // Add first item with its group title
            val firstItem = data.first()
            val firstDate = firstItem.timeStampInMillis.toDateFormatWithTodayYesterday()

            items.add(
                TxHistoryUM.TxHistoryItemUM.GroupTitle(
                    title = firstDate,
                    itemKey = UUID.randomUUID().toString(),
                ),
            )
            items.add(TxHistoryUM.TxHistoryItemUM.Transaction(txHistoryItemConverter.convert(firstItem)))

            // Process remaining items with date separators when needed
            data.zipWithNext { current, next ->
                val currentDate = current.timeStampInMillis.toDateFormatWithTodayYesterday()
                val nextDate = next.timeStampInMillis.toDateFormatWithTodayYesterday()

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
        txInfos: List<TangemPayTxHistoryItem>,
    ): Boolean {
        return this.filterIsInstance<TxHistoryUM.TxHistoryItemUM.Transaction>().size != txInfos.size
    }
}