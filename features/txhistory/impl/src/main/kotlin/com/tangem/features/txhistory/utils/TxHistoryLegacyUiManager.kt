package com.tangem.features.txhistory.utils

import com.tangem.core.ui.utils.toDateFormatWithTodayYesterday
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.txhistory.models.PaginationWrapper
import com.tangem.features.txhistory.converter.TxHistoryItemToTransactionStateConverter
import com.tangem.features.txhistory.entity.TxHistoryUM
import com.tangem.pagination.Batch
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.annotations.RemoveWithToggle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

@Deprecated("Remove with main toggle [DesignFeatureToggles.isRedesignEnabled]. Renders pre-redesign tx-history UI.")
@RemoveWithToggle("APP_REDESIGN_ENABLED")
internal class TxHistoryLegacyUiManager(
    private val state: MutableStateFlow<TxHistoryListState>,
    private val txHistoryItemConverter: TxHistoryItemToTransactionStateConverter,
    private val txHistoryUiActions: TxHistoryUiActions,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    val items: Flow<ImmutableList<TxHistoryUM.TxHistoryItemUM>> = state
        .filter { state ->
            state.status !is PaginationStatus.None &&
                state.status !is PaginationStatus.InitialLoading &&
                state.status !is PaginationStatus.InitialLoadingError
        }
        .mapLatest { state ->
            state.legacyUiBatches.asSequence()
                .flatMap { it.data }
                .toImmutableList()
        }
        .distinctUntilChanged()

    fun createOrUpdateUiBatches(
        newCurrencyBatches: List<Batch<Int, PaginationWrapper<TxInfo>>>,
        shouldClearUiBatches: Boolean,
    ): List<Batch<Int, List<TxHistoryUM.TxHistoryItemUM>>> {
        val currentUiBatches = state.value.legacyUiBatches
        val batches = if (shouldClearUiBatches) mutableListOf() else currentUiBatches.toMutableList()

        for ((key, data) in newCurrencyBatches) {
            val existingBatchIndex = batches.indexOfFirst { it.key == key }
            if (existingBatchIndex == -1) {
                val items = generateUiItems(key, data)
                batches.add(Batch(key = key, data = items))
            } else if (currentUiBatches[existingBatchIndex].data.transactionItemsSizeNotEqual(data.items)) {
                val items = generateUiItems(key, data)
                batches[existingBatchIndex] = Batch(key = key, data = items)
            }
        }

        return batches
    }

    private fun generateUiItems(key: Int, data: PaginationWrapper<TxInfo>): List<TxHistoryUM.TxHistoryItemUM> {
        val items = mutableListOf<TxHistoryUM.TxHistoryItemUM>()

        if (key == 0) {
            items.add(TxHistoryUM.TxHistoryItemUM.Title(onExploreClick = txHistoryUiActions::openExplorer))
        }

        if (data.items.isNotEmpty()) {
            val firstItem = data.items.first()
            val firstDate = firstItem.timestampInMillis.toDateFormatWithTodayYesterday()

            items.add(
                TxHistoryUM.TxHistoryItemUM.GroupTitle(
                    title = firstDate,
                    itemKey = "$key-$firstDate",
                ),
            )
            items.add(TxHistoryUM.TxHistoryItemUM.Transaction(txHistoryItemConverter.convert(firstItem)))

            data.items.zipWithNext { current, next ->
                val currentDate = current.timestampInMillis.toDateFormatWithTodayYesterday()
                val nextDate = next.timestampInMillis.toDateFormatWithTodayYesterday()

                if (currentDate != nextDate) {
                    items.add(
                        TxHistoryUM.TxHistoryItemUM.GroupTitle(
                            title = nextDate,
                            itemKey = "$key-$nextDate",
                        ),
                    )
                }
                items.add(TxHistoryUM.TxHistoryItemUM.Transaction(txHistoryItemConverter.convert(next)))
            }
        }

        return items
    }

    private fun List<TxHistoryUM.TxHistoryItemUM>.transactionItemsSizeNotEqual(txInfos: List<TxInfo>): Boolean {
        return this.filterIsInstance<TxHistoryUM.TxHistoryItemUM.Transaction>().size != txInfos.size
    }
}