package com.tangem.features.tangempay.utils

import com.tangem.core.ui.utils.toDateFormatWithTodayYesterday
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.features.tangempay.entity.TangemPayTxHistoryUM
import com.tangem.features.tangempay.model.transformers.TangemPayTxHistoryItemsConverter
import com.tangem.pagination.Batch
import com.tangem.pagination.PaginationStatus
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

internal class TangemPayTxHistoryUiManager(
    private val state: MutableStateFlow<TangemPayTxHistoryState>,
    private val txHistoryUiActions: TangemPayTxHistoryUiActions,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    val items: Flow<ImmutableList<TangemPayTxHistoryUM.TangemPayTxHistoryItemUM>> = state
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
    ): List<Batch<Int, List<TangemPayTxHistoryUM.TangemPayTxHistoryItemUM>>> {
        val currentUiBatches = state.value.uiBatches
        val batches = if (clearUiBatches) mutableListOf() else currentUiBatches.toMutableList()

        val rebucketed = rebucketByDate(newCurrencyBatches)

        for ((key, data) in rebucketed) {
            // Find if batch with same key exists
            val existingBatchIndex = batches.indexOfFirst { it.key == key }
            val shouldUpdateExisting = existingBatchIndex != -1 &&
                currentUiBatches[existingBatchIndex].data.transactionItemsDiffer(data)

            // Last date of previous batch's data, used to dedupe group title at the seam
            val previousLastDate = if (key > 0) {
                rebucketed.find { it.key == key - 1 }
                    ?.data?.lastOrNull()?.date?.millis?.toDateFormatWithTodayYesterday()
            } else {
                null
            }

            // Case 1: Update existing batch if contents differ
            if (shouldUpdateExisting) {
                val items = generateUiItems(key, data, previousLastDate)
                batches[existingBatchIndex] = Batch(key = key, data = items)
                continue
            }

            // Case 2: Skip if batch exists and has same contents
            if (existingBatchIndex != -1) {
                continue
            }

            // Case 3: Create new batch
            val items = generateUiItems(key, data, previousLastDate)
            batches.add(Batch(key = key, data = items))
        }

        return batches
    }

    private fun rebucketByDate(
        batches: List<Batch<Int, List<TangemPayTxHistoryItem>>>,
    ): List<Batch<Int, List<TangemPayTxHistoryItem>>> {
        val sortedItems = batches.asSequence()
            .flatMap { it.data.asSequence() }
            .sortedByDescending { it.date.millis }
            .toList()

        var offset = 0
        return batches.map { (key, data) ->
            val chunk = sortedItems.subList(offset, offset + data.size)
            offset += data.size
            Batch(key = key, data = chunk)
        }
    }

    private fun generateUiItems(
        key: Int,
        data: List<TangemPayTxHistoryItem>,
        previousLastDate: String? = null,
    ): List<TangemPayTxHistoryUM.TangemPayTxHistoryItemUM> {
        val items = mutableListOf<TangemPayTxHistoryUM.TangemPayTxHistoryItemUM>()

        // Add title for the first batch
        if (key == 0) {
            items.add(TangemPayTxHistoryUM.TangemPayTxHistoryItemUM.Title)
        }

        // Process batch items only if there are any
        if (data.isNotEmpty()) {
            // Add first item with its group title
            val firstItem = data.first()
            val firstDate = firstItem.date.millis.toDateFormatWithTodayYesterday()

            // Only add group title if different from previous batch's last date
            if (firstDate != previousLastDate) {
                items.add(
                    TangemPayTxHistoryUM.TangemPayTxHistoryItemUM.GroupTitle(
                        title = firstDate,
                        itemKey = "title-$firstDate",
                    ),
                )
            }
            items.add(
                TangemPayTxHistoryUM.TangemPayTxHistoryItemUM.Transaction(txHistoryItemConverter.convert(firstItem)),
            )

            // Process remaining items with date separators when needed
            data.zipWithNext { current, next ->
                val currentDate = current.date.millis.toDateFormatWithTodayYesterday()
                val nextDate = next.date.millis.toDateFormatWithTodayYesterday()

                if (currentDate != nextDate) {
                    items.add(
                        TangemPayTxHistoryUM.TangemPayTxHistoryItemUM.GroupTitle(
                            title = nextDate,
                            itemKey = "title-$nextDate",
                        ),
                    )
                }
                items.add(
                    TangemPayTxHistoryUM.TangemPayTxHistoryItemUM.Transaction(txHistoryItemConverter.convert(next)),
                )
            }
        }

        return items
    }

    private fun List<TangemPayTxHistoryUM.TangemPayTxHistoryItemUM>.transactionItemsDiffer(
        txInfos: List<TangemPayTxHistoryItem>,
    ): Boolean {
        val existingIds = this
            .asSequence()
            .filterIsInstance<TangemPayTxHistoryUM.TangemPayTxHistoryItemUM.Transaction>()
            .map { it.transaction.id }
            .toList()
        return existingIds != txInfos.map { it.id }
    }
}