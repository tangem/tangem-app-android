package com.tangem.features.txhistory.utils

import com.tangem.core.ui.utils.toDateFormatWithTodayYesterday
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.txhistory.model.identityKey
import com.tangem.domain.txhistory.models.PaginationWrapper
import com.tangem.features.txhistory.converter.TxHistoryItemToTransactionItemUMConverter
import com.tangem.features.txhistory.entity.TxHistoryItemsUM
import com.tangem.pagination.Batch
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.annotations.RemoveWithToggle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

@Deprecated("Remove with toggle [TxHistoryFeatureToggles.isNewTxHistoryEnabled]. Used only by TxHistoryListManager.")
@RemoveWithToggle("AND_15767_NEW_TX_HISTORY_ENABLED")
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

        for ((key, data) in newCurrencyBatches) {
            val uniqueItems = data.items.filter { seenTxIds.add(it.identityKey()) }
            val existingBatchIndex = batches.indexOfFirst { it.key == key }
            if (existingBatchIndex == -1) {
                val items = generateUiItems(key, data.copy(items = uniqueItems), converter)
                batches.add(Batch(key = key, data = items))
            } else if (currentUiBatches[existingBatchIndex].data.transactionItemsSizeNotEqual(uniqueItems)) {
                val items = generateUiItems(key, data.copy(items = uniqueItems), converter)
                batches[existingBatchIndex] = Batch(key = key, data = items)
            }
        }

        return batches
    }

    private fun generateUiItems(
        key: Int,
        data: PaginationWrapper<TxInfo>,
        converter: TxHistoryItemToTransactionItemUMConverter,
    ): List<TxHistoryItemsUM.TxHistoryItemUM> {
        val items = mutableListOf<TxHistoryItemsUM.TxHistoryItemUM>()

        if (data.items.isNotEmpty()) {
            val firstItem = data.items.first()
            val firstDate = firstItem.timestampInMillis.toDateFormatWithTodayYesterday()

            items.add(
                TxHistoryItemsUM.TxHistoryItemUM.GroupTitle(
                    title = firstDate,
                    itemKey = "$key-$firstDate",
                ),
            )
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