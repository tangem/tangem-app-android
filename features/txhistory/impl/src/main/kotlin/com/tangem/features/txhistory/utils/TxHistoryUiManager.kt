package com.tangem.features.txhistory.utils

import com.tangem.core.ui.utils.toDateFormatWithTodayYesterday
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.txhistory.models.PaginationWrapper
import com.tangem.features.txhistory.converter.TxHistoryItemToTransactionStateConverter
import com.tangem.features.txhistory.entity.TxHistoryUM
import com.tangem.pagination.Batch
import com.tangem.pagination.PaginationStatus
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.util.UUID

internal class TxHistoryUiManager(
    private val state: MutableStateFlow<TxHistoryListState>,
    private val txHistoryItemConverter: TxHistoryItemToTransactionStateConverter,
    private val txHistoryUiActions: TxHistoryUiActions,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    val items: Flow<ImmutableList<TxHistoryUM.TxHistoryItemUM>> = state
        // filter initial states, since we dont emit loading items as UI items
        .filter { state ->
            state.status !is PaginationStatus.None &&
                state.status !is PaginationStatus.InitialLoading &&
                state.status !is PaginationStatus.InitialLoadingError
        }
        .mapLatest { state ->
            state.uiBatches.asSequence()
                .flatMap { it.data }
                .toImmutableList()
        }
        .distinctUntilChanged()

    fun createOrUpdateUiBatches(
        newCurrencyBatches: List<Batch<Int, PaginationWrapper<TxInfo>>>,
    ): List<Batch<Int, List<TxHistoryUM.TxHistoryItemUM>>> {
        val batches = mutableListOf<Batch<Int, List<TxHistoryUM.TxHistoryItemUM>>>()

        for ((key, data) in newCurrencyBatches) {
            val previousBatchLastDate = batches.lastGroupTitleDate()
            val items = generateUiItems(key = key, data = data, previousBatchLastDate = previousBatchLastDate)
            batches.add(Batch(key = key, data = items))
        }

        return batches
    }

    private fun generateUiItems(
        key: Int,
        data: PaginationWrapper<TxInfo>,
        previousBatchLastDate: String?,
    ): List<TxHistoryUM.TxHistoryItemUM> = buildList {
        if (key == 0) {
            add(TxHistoryUM.TxHistoryItemUM.Title(onExploreClick = txHistoryUiActions::openExplorer))
        }
        var lastDate = previousBatchLastDate
        for (txInfo in data.items) {
            val date = txInfo.timestampInMillis.toDateFormatWithTodayYesterday()
            if (date != lastDate) {
                add(TxHistoryUM.TxHistoryItemUM.GroupTitle(title = date, itemKey = UUID.randomUUID().toString()))
                lastDate = date
            }
            add(TxHistoryUM.TxHistoryItemUM.Transaction(txHistoryItemConverter.convert(txInfo)))
        }
    }

    private fun List<Batch<Int, List<TxHistoryUM.TxHistoryItemUM>>>.lastGroupTitleDate(): String? {
        return lastOrNull()
            ?.data
            ?.filterIsInstance<TxHistoryUM.TxHistoryItemUM.GroupTitle>()
            ?.lastOrNull()
            ?.title
    }
}