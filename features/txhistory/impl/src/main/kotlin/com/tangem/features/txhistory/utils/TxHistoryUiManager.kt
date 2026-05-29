package com.tangem.features.txhistory.utils

import com.tangem.core.ui.utils.toDateFormatWithTodayYesterday
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.txhistory.models.PaginationWrapper
import com.tangem.features.txhistory.converter.TxHistoryItemToTransactionItemUMConverter
import com.tangem.features.txhistory.entity.TxHistoryItemsUM
import com.tangem.pagination.Batch
import com.tangem.pagination.PaginationStatus
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

internal class TxHistoryUiManager(
    private val state: MutableStateFlow<TxHistoryListState>,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    val items: Flow<ImmutableList<TxHistoryItemsUM.TxHistoryItemUM>> = state
        .filter { it.hasContent }
        .mapLatest { state ->
            state.uiBatches.asSequence()
                .flatMap { it.data }
                .toImmutableList()
        }
        .distinctUntilChanged()

    fun createOrUpdateUiBatches(
        newCurrencyBatches: List<Batch<Int, PaginationWrapper<TxInfo>>>,
        shouldClearUiBatches: Boolean,
        converter: TxHistoryItemToTransactionItemUMConverter,
    ): List<Batch<Int, List<TxHistoryItemsUM.TxHistoryItemUM>>> {
        val currentUiBatches = state.value.uiBatches
        val batches = if (shouldClearUiBatches) mutableListOf() else currentUiBatches.toMutableList()
        val seenTxIds = mutableSetOf<String>()
        var previousBatchLastDate: String? = null

        for ((key, data) in newCurrencyBatches) {
            val uniqueItems = data.items.filter { seenTxIds.add(it.identityKey()) }
            val existingBatchIndex = batches.indexOfFirst { it.key == key }
            if (existingBatchIndex == -1) {
                val items = generateUiItems(key, data.copy(items = uniqueItems), converter, previousBatchLastDate)
                batches.add(Batch(key = key, data = items))
            } else if (currentUiBatches[existingBatchIndex].data.transactionItemsSizeNotEqual(uniqueItems)) {
                val items = generateUiItems(key, data.copy(items = uniqueItems), converter, previousBatchLastDate)
                batches[existingBatchIndex] = Batch(key = key, data = items)
            }
            previousBatchLastDate = data.items.lastOrNull()
                ?.timestampInMillis
                ?.toDateFormatWithTodayYesterday()
                ?: previousBatchLastDate
        }

        return batches
    }

    private fun generateUiItems(
        key: Int,
        data: PaginationWrapper<TxInfo>,
        converter: TxHistoryItemToTransactionItemUMConverter,
        previousBatchLastDate: String?,
    ): List<TxHistoryItemsUM.TxHistoryItemUM> {
        val items = mutableListOf<TxHistoryItemsUM.TxHistoryItemUM>()

        if (data.items.isNotEmpty()) {
            val firstItem = data.items.first()
            val firstDate = firstItem.timestampInMillis.toDateFormatWithTodayYesterday()

            if (firstDate != previousBatchLastDate) {
                items.add(
                    TxHistoryItemsUM.TxHistoryItemUM.GroupTitle(
                        title = firstDate,
                        itemKey = "$key-$firstDate",
                    ),
                )
            }
            items.add(TxHistoryItemsUM.TxHistoryItemUM.Transaction(converter.convert(firstItem)))

            data.items.zipWithNext { current, next ->
                val currentDate = current.timestampInMillis.toDateFormatWithTodayYesterday()
                val nextDate = next.timestampInMillis.toDateFormatWithTodayYesterday()

                if (currentDate != nextDate) {
                    items.add(
                        TxHistoryItemsUM.TxHistoryItemUM.GroupTitle(
                            title = nextDate,
                            itemKey = "$key-$nextDate",
                        ),
                    )
                }
                items.add(TxHistoryItemsUM.TxHistoryItemUM.Transaction(converter.convert(next)))
            }
        }

        return items
    }

    private fun List<TxHistoryItemsUM.TxHistoryItemUM>.transactionItemsSizeNotEqual(txInfos: List<TxInfo>): Boolean {
        return this.filterIsInstance<TxHistoryItemsUM.TxHistoryItemUM.Transaction>().size != txInfos.size
    }
}

private val TxHistoryListState.hasContent: Boolean
    get() = status !is PaginationStatus.None &&
        status !is PaginationStatus.InitialLoading &&
        status !is PaginationStatus.InitialLoadingError

/**
 * Cross-batch identity of a tx: `txHash` alone is not enough because gasless flows surface several
 * events under the same on-chain hash (e.g. `GaslessFee` + `Transfer`). Pinning the [TxInfo.type]
 * keeps those legitimate sibling events apart while still collapsing the same event seen twice —
 * e.g. an Unconfirmed copy injected via `addRecentTransactions` and a Confirmed copy that arrives
 * in a later API batch.
 */
private fun TxInfo.identityKey(): String = "$txHash|$type"